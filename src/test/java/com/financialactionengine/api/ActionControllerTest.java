package com.financialactionengine.api;

import com.financialactionengine.api.converter.ActionConverter;
import com.financialactionengine.api.dto.CreateActionRequest;
import com.financialactionengine.api.dto.CreateActionResponse;
import com.financialactionengine.application.CreateActionService;
import com.financialactionengine.domain.Action;
import com.financialactionengine.domain.ActionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionControllerTest {

    @Mock
    private CreateActionService createActionService;

    @Mock
    private ActionConverter actionConverter;

    @InjectMocks
    private ActionController controller;

    @Test
    void shouldCreateActionUsingClientIdClaimAndReturnConvertedResponse() {
        var request = new CreateActionRequest();
        var clientId = "client-123";
        var idempotencyKey = "action-456";
        var authentication = authenticationFor(clientId);
        var createdAction = Action.builder().build();
        var response = CreateActionResponse.builder()
                .id(42L)
                .amount(new BigDecimal("150.75"))
                .currency(Currency.getInstance("BRL"))
                .status(ActionStatus.PROPOSED)
                .createdAt(LocalDateTime.of(2026, 10, 19, 10, 30))
                .build();

        when(createActionService.create(request, clientId, idempotencyKey)).thenReturn(createdAction);
        when(actionConverter.buildActionResponse(createdAction)).thenReturn(response);

        var result = controller.createAction(request, idempotencyKey, authentication);

        assertEquals(response, result);
        verify(createActionService).create(request, clientId, idempotencyKey);
        verify(actionConverter).buildActionResponse(createdAction);
    }

    @Test
    void shouldPropagateServiceExceptionWithoutCallingConverter() {
        var request = new CreateActionRequest();
        var clientId = "client-123";
        var idempotencyKey = "action-456";
        var authentication = authenticationFor(clientId);
        var exception = new DataIntegrityViolationException("Duplicate idempotency key");
        when(createActionService.create(request, clientId, idempotencyKey)).thenThrow(exception);

        var thrown = assertThrows(DataIntegrityViolationException.class,
                () -> controller.createAction(request, idempotencyKey, authentication));

        assertEquals(exception, thrown);
        verify(createActionService).create(request, clientId, idempotencyKey);
        verifyNoInteractions(actionConverter);
    }

    private JwtAuthenticationToken authenticationFor(String clientId) {
        var token = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-789")
                .claim("client_id", clientId)
                .build();
        return new JwtAuthenticationToken(token);
    }
}
