package com.familyhome.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
