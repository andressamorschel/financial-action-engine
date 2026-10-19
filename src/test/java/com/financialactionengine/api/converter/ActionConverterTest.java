package com.financialactionengine.api.converter;

import com.financialactionengine.domain.Action;
import com.financialactionengine.domain.ActionStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionConverterTest {

    private final ActionConverter converter = new ActionConverter();

    @ParameterizedTest
    @EnumSource(ActionStatus.class)
    void shouldMapAllActionFieldsToResponse(ActionStatus status) {
        var action = mock(Action.class);
        var amount = new BigDecimal("150.75");
        var currency = Currency.getInstance("BRL");
        var createdAt = LocalDateTime.of(2026, 10, 19, 10, 30);

        when(action.getId()).thenReturn(42L);
        when(action.getAmount()).thenReturn(amount);
        when(action.getCurrency()).thenReturn(currency);
        when(action.getStatus()).thenReturn(status);
        when(action.getCreatedAt()).thenReturn(createdAt);

        var response = converter.buildActionResponse(action);

        assertAll(
                () -> assertEquals(42L, response.id()),
                () -> assertEquals(amount, response.amount()),
                () -> assertEquals(currency, response.currency()),
                () -> assertEquals(status, response.status()),
                () -> assertEquals(createdAt, response.createdAt())
        );
    }
}
