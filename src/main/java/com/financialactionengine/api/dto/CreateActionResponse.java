package com.financialactionengine.api.dto;

import com.financialactionengine.domain.ActionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

@Builder
public record CreateActionResponse(
        Long id,
        BigDecimal amount,
        Currency currency,
        ActionStatus status,
        LocalDateTime createdAt
) {
}
