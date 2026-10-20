package com.financialactionengine.integration.fixture;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public final class ActionFixture {

    public static Response createAction(String payload) {
        return createAction(payload, "test-token", "action-456");
    }

    public static Response createAction(String payload, String token, String idempotencyKey) {
        var request = given().body(payload);
        if (token != null) {
            request.auth()
                   .oauth2(token);
        }
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.when()
                      .post("/api/v1/actions");
    }
}
