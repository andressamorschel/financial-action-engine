package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import com.financialactionengine.domain.Action;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import org.junit.jupiter.api.Test;

import static org.springframework.test.util.ReflectionTestUtils.setField;
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

    @Mock
    private ActionValidator actionValidator;

    @InjectMocks
    private CreateActionService service;

    @Test
    void shouldSaveActionWithRequestFieldsAndDefaultsAndReturnSavedAction() {
        var request = new CreateActionRequest();
        var amount = new BigDecimal("150.75");
        var currency = Currency.getInstance("BRL");
        var clientId = "client-123";
        var idempotencyKey = "action-456";
        var savedAction = mock(Action.class);

        setField(request, "amount", amount);
        setField(request, "description", "Transfer between accounts");
        setField(request, "currency", currency);
        setField(request, "debitAccountId", "debit-123");
        setField(request, "creditAccountId", "credit-456");
        when(actionRepository.save(any(Action.class))).thenReturn(savedAction);
        when(actionRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey))
                .thenReturn(Optional.empty());

        var result = service.create(request, clientId, idempotencyKey);
        var actionCaptor = ArgumentCaptor.forClass(Action.class);

        verify(actionValidator).validate(request);
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
        var action = existingAction();
        var request = matchingRequest(action);
        setField(request, "amount", action.getAmount());
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
        setField(request, "amount", new BigDecimal(amount));
        when(actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456"))
                .thenReturn(Optional.of(action));

        var result = service.create(request, "client-123", "action-456");

        assertSame(action, result);
        verifyNoInteractions(actionValidator);
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

        var exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request, "client-123", "action-456"));
        assertEquals(CONFLICT, exception.getStatusCode());

        verify(actionRepository).findByClientIdAndIdempotencyKey("client-123", "action-456");
        verify(actionRepository, never()).save(any(Action.class));
    }

    @Test
    void shouldPropagateExceptionWhenValidationFailsWithoutSaving() {
        var request = new CreateActionRequest();
        var validationException = new ResponseStatusException(BAD_REQUEST, "Invalid financial action fields.");
        doThrow(validationException).when(actionValidator).validate(request);

        var exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request, "client-123", "new-key"));
        assertSame(validationException, exception);
        assertEquals(BAD_REQUEST, exception.getStatusCode());
        verify(actionValidator).validate(request);
        verify(actionRepository, never()).save(any());
    }

    @Test
    void shouldTreatMissingAmountOnRetryAsConflict() {
        when(actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456"))
                .thenReturn(Optional.of(existingAction()));
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new CreateActionRequest(), "client-123", "action-456"));
        assertEquals(CONFLICT, exception.getStatusCode());
        verifyNoInteractions(actionValidator);
        verify(actionRepository, never()).save(any());
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
        var request = matchingRequest(action);
        Object value = switch (field) {
            case "amount" -> new BigDecimal("200.00");
            case "currency" -> Currency.getInstance("USD");
            case "description" -> "Different description";
            case "debitAccountId" -> "different-debit";
            case "creditAccountId" -> "different-credit";
            default -> throw new IllegalArgumentException(field);
        };
        setField(request, field, value);
        return request;
    }

    private CreateActionRequest matchingRequest(Action action) {
        var request = new CreateActionRequest();
        setField(request, "amount", action.getAmount());
        setField(request, "currency", action.getCurrency());
        setField(request, "description", action.getDescription());
        setField(request, "debitAccountId", action.getDebitAccountId());
        setField(request, "creditAccountId", action.getCreditAccountId());
        return request;
    }
}
