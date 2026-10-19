package com.financialactionengine.api;

import com.financialactionengine.api.converter.ActionConverter;
import com.financialactionengine.api.dto.CreateActionRequest;
import com.financialactionengine.api.dto.CreateActionResponse;
import com.financialactionengine.application.CreateActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/actions")
public class ActionController {

    private final CreateActionService createActionService;

    private final ActionConverter actionConverter;

    @PostMapping
    @ResponseStatus(CREATED)
    public CreateActionResponse createAction(@RequestBody @Valid CreateActionRequest request,
                                             @RequestHeader("Idempotency-Key") String idempotencyKey,
                                             JwtAuthenticationToken authentication
    ) {
        var clientId = authentication.getToken()
                .getClaimAsString("client_id");

        var createdAction = createActionService.create(request, clientId, idempotencyKey);

        return actionConverter.buildActionResponse(createdAction);
    }
}
