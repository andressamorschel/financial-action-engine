package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import com.financialactionengine.domain.Action;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Currency;

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

        var result = service.create(request, clientId, idempotencyKey);
        var actionCaptor = ArgumentCaptor.forClass(Action.class);

        verify(actionRepository).save(actionCaptor.capture());
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
}
