package com.financialactionengine.integration.config;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;


import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class IntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @BeforeEach
    void configureHttp() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
        when(jwtDecoder.decode("test-token"))
                .thenReturn(jwt("test-token", "client-123", List.of("actions:create")));
        when(jwtDecoder.decode("other-client-token"))
                .thenReturn(jwt("other-client-token", "client-789", List.of("actions:create")));
        when(jwtDecoder.decode("without-authority-token"))
                .thenReturn(jwt("without-authority-token", "client-123", List.of()));
    }

    @AfterEach
    void resetHttp() {
        RestAssured.reset();
    }

    private Jwt jwt(String token, String clientId, List<String> roles) {
        return Jwt.withTokenValue(token)
                  .header("alg", "RS256")
                  .subject("user-789")
                  .claim("client_id", clientId)
                  .claim("resource_access", Map.of("financial-action-agent", Map.of("roles", roles)))
                  .build();
    }

}
