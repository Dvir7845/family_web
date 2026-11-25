package com.familyhome.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
