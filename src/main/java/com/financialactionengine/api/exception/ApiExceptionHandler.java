package com.financialactionengine.api.exception;

import com.financialactionengine.application.exception.IdempotencyConflictException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(
            IdempotencyConflictException exception
    ) {
        return ResponseEntity.status(CONFLICT).body(forStatusAndDetail(
                CONFLICT,
                exception.getMessage()
        ));
    }
}
