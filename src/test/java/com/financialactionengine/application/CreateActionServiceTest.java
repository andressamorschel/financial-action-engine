package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import com.financialactionengine.application.exception.IdempotencyConflictException;
import com.financialactionengine.domain.Action;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static com.financialactionengine.domain.ActionSource.AI_AGENT;
import static com.financialactionengine.domain.ActionStatus.PROPOSED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateActionServiceTest {

    @Mock
    private ActionRepository actionRepository;

    @InjectMocks
    private CreateActionService service;

    @Test
    void shouldSaveActionWithRequestFieldsAndDefaultsAndReturnSavedAction() {
        var request = mock(CreateActionRequest.class);
        var amount = new BigDecimal("150.75");
        var currency = Currency.getInstance("BRL");
        var clientId = "client-123";
        var idempotencyKey = "action-456";
        var savedAction = mock(Action.class);

        when(request.getAmount()).thenReturn(amount);
        when(request.getDescription()).thenReturn("Transfer between accounts");
        when(request.getCurrency()).thenReturn(currency);
        when(request.getDebitAccountId()).thenReturn("debit-123");
        when(request.getCreditAccountId()).thenReturn("credit-456");
        when(actionRepository.save(any(Action.class))).thenReturn(savedAction);
        when(actionRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey))
                .thenReturn(Optional.empty());

        var result = service.create(request, clientId, idempotencyKey);
        var actionCaptor = ArgumentCaptor.forClass(Action.class);

        verify(actionRepository).save(actionCaptor.capture());
        verify(actionRepository).findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
        var action = actionCaptor.getValue();

        assertEquals(amount, action.getAmount());
        assertEquals("Transfer between accounts", action.getDescription());
        assertEquals(currency, action.getCurrency());
        assertEquals("debit-123", action.getDebitAccountId());
        assertEquals("credit-456", action.getCreditAccountId());
        assertEquals(clientId, action.getClientId());
        assertEquals(idempotencyKey, action.getIdempotencyKey());
        assertEquals(AI_AGENT, action.getSource());
        assertEquals(PROPOSED, action.getStatus());
        assertSame(savedAction, result);
    }

    @Test
    void shouldPropagateRepositoryException() {
        var request = new CreateActionRequest();
        var exception = new DataIntegrityViolationException("Duplicate idempotency key");
        when(actionRepository.save(any(Action.class))).thenThrow(exception);

        var thrown = assertThrows(DataIntegrityViolationException.class,
                () -> service.create(request, "client-123", "action-456"));

        assertSame(exception, thrown);
        verify(actionRepository).save(any(Action.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"150.75", "150.750"})
    void shouldReturnExistingActionWhenRequestMatchesRegardlessOfAmountScale(String amount) {
        var action = existingAction();
        var request = matchingRequest(action);
        when(request.getAmount()).thenReturn(new BigDecimal(amount));
        when(actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456"))
                .thenReturn(Optional.of(action));

        var result = service.create(request, "client-123", "action-456");

        assertSame(action, result);
        verify(actionRepository).findByClientIdAndIdempotencyKey("client-123", "action-456");
        verify(actionRepository, never()).save(any(Action.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency", "description", "debitAccountId", "creditAccountId"})
    void shouldRejectExistingIdempotencyKeyWhenAnyRequestFieldDiffers(String field) {
        var action = existingAction();
        var request = conflictingRequest(action, field);
        when(actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456"))
                .thenReturn(Optional.of(action));

        assertThrows(IdempotencyConflictException.class,
                () -> service.create(request, "client-123", "action-456"));

        verify(actionRepository).findByClientIdAndIdempotencyKey("client-123", "action-456");
        verify(actionRepository, never()).save(any(Action.class));
    }

    private Action existingAction() {
        return Action.builder()
                     .amount(new BigDecimal("150.75"))
                     .currency(Currency.getInstance("BRL"))
                     .description("Transfer between accounts")
                     .debitAccountId("debit-123")
                     .creditAccountId("credit-456")
                     .clientId("client-123")
                     .idempotencyKey("action-456")
                     .build();
    }

    private CreateActionRequest conflictingRequest(Action action, String field) {
        var request = mock(CreateActionRequest.class);
        if (field.equals("amount")) {
            when(request.getAmount()).thenReturn(new BigDecimal("200.00"));
            return request;
        }
        when(request.getAmount()).thenReturn(action.getAmount());

        if (field.equals("currency")) {
            when(request.getCurrency()).thenReturn(Currency.getInstance("USD"));
            return request;
        }
        when(request.getCurrency()).thenReturn(action.getCurrency());

        if (field.equals("description")) {
            when(request.getDescription()).thenReturn("Different description");
            return request;
        }
        when(request.getDescription()).thenReturn(action.getDescription());

        if (field.equals("debitAccountId")) {
            when(request.getDebitAccountId()).thenReturn("different-debit");
            return request;
        }
        when(request.getDebitAccountId()).thenReturn(action.getDebitAccountId());
        when(request.getCreditAccountId()).thenReturn("different-credit");
        return request;
    }

    private CreateActionRequest matchingRequest(Action action) {
        var request = mock(CreateActionRequest.class);
        when(request.getCurrency()).thenReturn(action.getCurrency());
        when(request.getDescription()).thenReturn(action.getDescription());
        when(request.getDebitAccountId()).thenReturn(action.getDebitAccountId());
        when(request.getCreditAccountId()).thenReturn(action.getCreditAccountId());
        return request;
    }
}
