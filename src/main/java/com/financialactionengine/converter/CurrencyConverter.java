package com.financialactionengine.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Currency;
import java.util.Optional;

@Converter
public class CurrencyConverter
        implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        return Optional.ofNullable(currency)
                .map(Currency::getCurrencyCode)
                .orElse(null);
    }

    @Override
    public Currency convertToEntityAttribute(String currencyCode) {
        return Optional.ofNullable(currencyCode)
                .map(Currency::getInstance)
                .orElse(null);
    }
}
