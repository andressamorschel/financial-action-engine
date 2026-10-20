package com.financialactionengine.integration;

import com.financialactionengine.integration.config.IntegrationTest;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.financialactionengine.integration.fixture.ActionFixture.createAction;
import static org.assertj.core.api.Assertions.assertThat;

class ActionIntegrationTest extends IntegrationTest {

    @Autowired
    private ActionRepository actionRepository;

    @BeforeEach
    void setUp() {
        actionRepository.deleteAll();
    }

    @Test
    void shouldCreateAndPersistAction() {
        var response = createAction(requestBody("150.75"));

        assertThat(response.statusCode()).isEqualTo(201);
        var body = response.jsonPath();
        assertThat(body.getLong("id")).isPositive();
        assertThat(body.getFloat("amount")).isEqualTo(150.75f);
        assertThat(body.getString("currency")).isEqualTo("BRL");
        assertThat(body.getString("status")).isEqualTo("PROPOSED");
        assertThat(body.getString("createdAt")).isNotBlank();

        var action = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456")
                                     .orElseThrow();

        assertThat(action.getDescription()).isEqualTo("Transfer between accounts");
        assertThat(action.getDebitAccountId()).isEqualTo("debit-123");
        assertThat(action.getCreditAccountId()).isEqualTo("credit-456");
        assertThat(actionRepository.count()).isEqualTo(1L);
    }

    @Test
    void shouldReturnSameActionForRepeatedRequest() {
        var payload = requestBody("150.75");
        var firstResponse = createAction(payload);
        var secondResponse = createAction(payload);

        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(secondResponse.statusCode()).isEqualTo(201);
        assertThat(secondResponse.jsonPath()
                                 .getLong("id"))
                .isEqualTo(firstResponse.jsonPath()
                                        .getLong("id"));
        assertThat(secondResponse.jsonPath()
                                 .getString("createdAt"))
                .isEqualTo(firstResponse.jsonPath()
                                        .getString("createdAt"));
        assertThat(actionRepository.count()).isEqualTo(1L);
    }

    @Test
    void shouldAllowSameIdempotencyKeyForDifferentClients() {
        var firstResponse = createAction(requestBody("150.75"));
        var secondResponse = createAction(requestBody("200.00"), "other-client-token", "action-456");

        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(secondResponse.statusCode()).isEqualTo(201);

        var firstAction = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456")
                                          .orElseThrow();
        var secondAction = actionRepository.findByClientIdAndIdempotencyKey("client-789", "action-456")
                                           .orElseThrow();

        assertThat(firstAction.getId()).isEqualTo(firstResponse.jsonPath()
                                                               .getLong("id"));
        assertThat(secondAction.getId()).isEqualTo(secondResponse.jsonPath()
                                                                 .getLong("id"));
        assertThat(firstAction.getAmount()).isEqualByComparingTo("150.75");
        assertThat(secondAction.getAmount()).isEqualByComparingTo("200.00");
        assertThat(actionRepository.count()).isEqualTo(2L);
    }

    @Test
    void shouldRejectDifferentPayloadForSameIdempotencyKey() {
        var firstResponse = createAction(requestBody("150.75"));
        var conflictResponse = createAction(requestBody("200.00"));

        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(conflictResponse.statusCode()).isEqualTo(409);
        assertThat(conflictResponse.jsonPath()
                                   .getInt("status")).isEqualTo(409);

        var action = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456")
                                     .orElseThrow();

        assertThat(action.getAmount()).isEqualByComparingTo("150.75");
        assertThat(actionRepository.count()).isEqualTo(1L);
    }

    @Test
    void shouldRejectInvalidAmount() {
        var response = createAction(requestBody("0.00"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(actionRepository.count()).isZero();
    }

    @Test
    void shouldRejectMissingIdempotencyKey() {
        var response = createAction(requestBody("150.75"), "test-token", null);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(actionRepository.count()).isZero();
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        var response = createAction(requestBody("150.75"), null, "action-456");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(actionRepository.count()).isZero();
    }

    @Test
    void shouldRejectClientWithoutCreateAuthority() {
        var response = createAction(requestBody("150.75"), "without-authority-token", "action-456");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(actionRepository.count()).isZero();
    }

    private String requestBody(String amount) {
        return """
                {
                  "amount": %s,
                  "currency": "BRL",
                  "description": "Transfer between accounts",
                  "debitAccountId": "debit-123",
                  "creditAccountId": "credit-456"
                }
                """.formatted(amount);
    }
}
