package com.shop.ecommerce.common.exception;

public class ExternalAuthenticationException extends RuntimeException {

    public ExternalAuthenticationException(String message) {
        super(message);
    }

    public ExternalAuthenticationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
