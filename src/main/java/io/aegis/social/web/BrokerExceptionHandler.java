package io.aegis.social.web;

import io.aegis.social.service.BrokerExceptions.DuplicateProviderException;
import io.aegis.social.service.BrokerExceptions.ProviderNotFoundException;
import io.aegis.social.service.BrokerExceptions.ProviderValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps identity-provider domain exceptions to RFC-7807 responses. */
@RestControllerAdvice
public class BrokerExceptionHandler {

    @ExceptionHandler(DuplicateProviderException.class)
    public ProblemDetail handleDuplicate(DuplicateProviderException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProviderNotFoundException.class)
    public ProblemDetail handleNotFound(ProviderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProviderValidationException.class)
    public ProblemDetail handleValidation(ProviderValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
