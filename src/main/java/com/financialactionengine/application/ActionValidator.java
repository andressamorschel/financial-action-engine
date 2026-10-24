package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionValidator {

    private final Validator validator;

    public void validate(CreateActionRequest request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid financial action fields.");
        }
        if (request.getDebitAccountId().equals(request.getCreditAccountId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Debit and credit accounts must differ.");
        }
    }
}
