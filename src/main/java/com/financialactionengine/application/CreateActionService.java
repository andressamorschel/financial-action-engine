package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import com.financialactionengine.domain.Action;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.financialactionengine.domain.ActionSource.AI_AGENT;
import static com.financialactionengine.domain.ActionStatus.PROPOSED;

@Service
@RequiredArgsConstructor
public class CreateActionService {

    private final ActionRepository actionRepository;

    @Transactional
    public Action create(
            CreateActionRequest request,
            String clientId,
            String idempotencyKey
    ) {
        var action = Action.builder()
                .amount(request.getAmount())
                .description(request.getDescription())
                .currency(request.getCurrency())
                .debitAccountId(request.getDebitAccountId())
                .creditAccountId(request.getCreditAccountId())
                .clientId(clientId)
                .idempotencyKey(idempotencyKey)
                .source(AI_AGENT)
                .status(PROPOSED)
                .build();

        return actionRepository.save(action);
    }
}
