package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import com.financialactionengine.domain.Action;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.financialactionengine.domain.ActionSource.AI_AGENT;
import static com.financialactionengine.domain.ActionStatus.PROPOSED;

@Service
@RequiredArgsConstructor
public class CreateActionService {

    private final ActionRepository actionRepository;

    private final ActionValidator actionValidator;

    @Transactional
    public Action create(
            CreateActionRequest request,
            String clientId,
            String idempotencyKey
    ) {
        var existingAction = actionRepository
                .findByClientIdAndIdempotencyKey(clientId, idempotencyKey);

        if (existingAction.isPresent()) {
            var action = existingAction.get();

            if (!matches(action, request)) {
                throw new ResponseStatusException(CONFLICT, "Idempotency key was already used with different action data");
            }

            return action;
        }

        actionValidator.validate(request);

        var action = buildAction(request, clientId, idempotencyKey);

        return actionRepository.save(action);
    }

    private static Action buildAction(CreateActionRequest request, String clientId, String idempotencyKey) {
        return Action.builder()
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
    }

    private boolean matches(
            Action action,
            CreateActionRequest request
    ) {
        return request != null && request.getAmount() != null
                && action.getAmount()
                     .compareTo(request.getAmount()) == 0
                && Objects.equals(action.getCurrency(), request.getCurrency())
                && Objects.equals(action.getDescription(), request.getDescription())
                && Objects.equals(action.getDebitAccountId(), request.getDebitAccountId())
                && Objects.equals(action.getCreditAccountId(), request.getCreditAccountId());
    }
}
