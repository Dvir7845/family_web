package com.familyhome.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
