package com.financialactionengine.application;

import com.financialactionengine.api.dto.CreateActionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ActionValidatorTest {

    @Mock
    private Validator beanValidator;

    @Mock
    private CreateActionRequest request;

    @Mock
    private ConstraintViolation<CreateActionRequest> violation;

    @InjectMocks
    private ActionValidator validator;

    @Test
    void shouldRejectWhenRequestIsNull() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Invalid financial action fields.");
                });
    }

    @Test
    void shouldRejectWhenDebitAndCreditAccountsAreEqual() {
        when(beanValidator.validate(request)).thenReturn(Set.of());
        when(request.getDebitAccountId()).thenReturn("account-123");
        when(request.getCreditAccountId()).thenReturn("account-123");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Debit and credit accounts must differ.");
                });
    }

    @Test
    void shouldAcceptWhenRequestIsValidAndAccountsAreDifferent() {
        when(beanValidator.validate(request)).thenReturn(Set.of());
        when(request.getDebitAccountId()).thenReturn("debit-123");
        when(request.getCreditAccountId()).thenReturn("credit-456");

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }
}
