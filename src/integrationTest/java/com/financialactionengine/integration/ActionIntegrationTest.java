package com.financialactionengine.integration;

import com.financialactionengine.integration.config.IntegrationTest;
import com.financialactionengine.infrastructure.persistence.ActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import com.financialactionengine.domain.Action;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(new java.math.BigDecimal(body.getString("amount"))).isEqualByComparingTo("150.75");
        assertThat(body.getString("currency")).isEqualTo("BRL");
        assertThat(body.getString("status")).isEqualTo("PROPOSED");
        assertThat(body.getString("createdAt")).isNotBlank();

        var action = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456")
                                     .orElseThrow();

        assertThat(action.getAmount()).isEqualByComparingTo("150.75");
        assertThat(action.getCurrency()).isEqualTo(java.util.Currency.getInstance("BRL"));
        assertThat(action.getClientId()).isEqualTo("client-123");
        assertThat(action.getIdempotencyKey()).isEqualTo("action-456");
        assertThat(action.getStatus()).isEqualTo(com.financialactionengine.domain.ActionStatus.PROPOSED);
        assertThat(action.getDescription()).isEqualTo("Transfer between accounts");
        assertThat(action.getDebitAccountId()).isEqualTo("debit-123");
        assertThat(action.getCreditAccountId()).isEqualTo("credit-456");
        assertThat(actionRepository.count()).isEqualTo(1L);
    }

    @Test
    void shouldReturnSameActionForRepeatedRequest() {
        var payload = requestBody("500.0");
        var firstResponse = createAction(payload);
        var secondResponse = createAction(requestBody("500.00"));

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

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00", "null", "1.001", "100000000000000000.00", "500.000"})
    void shouldReturnBadRequestWhenAmountIsInvalid(String amount) {
        var response = createAction(requestBody(amount));

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestWhenRequiredFieldIsInvalid(String scenario, String payload) {
        var response = createAction(payload);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("detail")).isEqualTo("Invalid financial action request.");
        assertThat(actionRepository.count()).isZero();
    }

    @Test
    void shouldReturnBadRequestWhenCurrencyCodeIsUnknown() {
        var response = createAction(requestBody("150.75").replace("BRL", "INVALID"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(actionRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "500.0", "500.00", "99999999999999999.99", "1E+2"})
    void shouldReturnCreatedWhenAmountIsValid(String amount) {
        var response = createAction(requestBody(amount));

        assertThat(response.statusCode()).isEqualTo(201);
        var action = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456").orElseThrow();
        assertThat(action.getAmount()).isEqualByComparingTo(new java.math.BigDecimal(amount));
        assertThat(actionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnCreatedWhenTextFieldsHaveMaximumLength() {
        var payload = requestBody("150.75")
                .replace("Transfer between accounts", "x".repeat(255))
                .replace("debit-123", "d".repeat(255))
                .replace("credit-456", "c".repeat(255));

        var response = createAction(payload);

        assertThat(response.statusCode()).isEqualTo(201);
        var action = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456").orElseThrow();
        assertThat(action.getDescription()).isEqualTo("x".repeat(255));
        assertThat(action.getDebitAccountId()).isEqualTo("d".repeat(255));
        assertThat(action.getCreditAccountId()).isEqualTo("c".repeat(255));
        assertThat(actionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnProblemDetailWhenDebitAndCreditAccountsAreIdentical() {
        var payload = requestBody("150.75").replace("credit-456", "debit-123");

        var response = createAction(payload);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.contentType()).startsWith("application/problem+json");
        var problem = response.jsonPath();
        assertThat(problem.getInt("status")).isEqualTo(400);
        assertThat(problem.getString("title")).isEqualTo("Bad Request");
        assertThat(problem.getString("detail")).isEqualTo("Debit and credit accounts must differ.");
        assertThat(problem.getString("instance")).isEqualTo("/api/v1/actions");
        assertThat(problem.getMap("$")).doesNotContainKeys("stackTrace", "exception", "trace");
        assertThat(response.body().asString()).doesNotContain("test-token", "ResponseStatusException");
        assertThat(actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456"))
                .isEmpty();
        assertThat(actionRepository.count()).isZero();
    }

    @Test
    void shouldEnforceDatabaseIdempotencyConstraint() {
        assertThat(createAction(requestBody("150.75")).statusCode()).isEqualTo(201);
        var existing = actionRepository.findByClientIdAndIdempotencyKey("client-123", "action-456").orElseThrow();
        var duplicate = Action.builder()
                .amount(existing.getAmount())
                .currency(existing.getCurrency())
                .description(existing.getDescription())
                .debitAccountId(existing.getDebitAccountId())
                .creditAccountId(existing.getCreditAccountId())
                .clientId(existing.getClientId())
                .idempotencyKey(existing.getIdempotencyKey())
                .source(existing.getSource())
                .status(existing.getStatus())
                .build();
        assertThatThrownBy(() -> actionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_action_client_idempotency_key");
        assertThat(actionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectConflictingRetryEvenWhenAccountsAreIdentical() {
        assertThat(createAction(requestBody("150.75")).statusCode()).isEqualTo(201);
        var response = createAction(requestBody("150.75").replace("credit-456", "debit-123"));
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(actionRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAllowValidRequestAfterInvalidRequestWithSameKey() {
        assertThat(createAction(requestBody("150.75").replace("credit-456", "debit-123")).statusCode())
                .isEqualTo(400);
        assertThat(createAction(requestBody("150.75")).statusCode()).isEqualTo(201);
        assertThat(actionRepository.count()).isEqualTo(1);
    }

    private static Stream<Arguments> invalidRequests() {
        var fields = Map.of(
                "amount", "150.75",
                "currency", "\"BRL\"",
                "description", "\"Transfer between accounts\"",
                "debitAccountId", "\"debit-123\"",
                "creditAccountId", "\"credit-456\"");

        var missingOrNull = fields.keySet().stream().flatMap(field -> {
            var missing = new LinkedHashMap<>(fields);
            missing.remove(field);
            var nullValue = new LinkedHashMap<>(fields);
            nullValue.put(field, "null");
            return Stream.of(
                    Arguments.of(field + " missing", jsonBody(missing)),
                    Arguments.of(field + " null", jsonBody(nullValue)));
        });
        var invalidText = Stream.of("description", "debitAccountId", "creditAccountId")
                .flatMap(field -> Stream.of("", " ", "x".repeat(256)).map(value -> {
                    var invalid = new LinkedHashMap<>(fields);
                    invalid.put(field, "\"" + value + "\"");
                    return Arguments.of(field + " length=" + value.length(), jsonBody(invalid));
                }));
        return Stream.concat(missingOrNull, invalidText);
    }

    private static String jsonBody(Map<String, String> jsonFields) {
        return jsonFields.entrySet().stream()
                .map(field -> "\"" + field.getKey() + "\":" + field.getValue())
                .collect(Collectors.joining(",", "{", "}"));
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
