package com.financialactionengine.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
public class CreateActionRequest {

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private Currency currency;

    @NotBlank
    private String creditAccountId;

    @NotBlank
    private String debitAccountId;
}
