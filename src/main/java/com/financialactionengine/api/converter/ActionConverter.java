package com.financialactionengine.api.converter;

import com.financialactionengine.api.dto.CreateActionResponse;
import com.financialactionengine.domain.Action;
import org.springframework.stereotype.Component;

@Component
public class ActionConverter {

    public CreateActionResponse buildActionResponse(Action action){
        return CreateActionResponse.builder()
                .id(action.getId())
                .amount(action.getAmount())
                .createdAt(action.getCreatedAt())
                .currency(action.getCurrency())
                .status(action.getStatus())
                .build();
    }
}
