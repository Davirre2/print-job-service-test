package com.adobe.printservice.web.exception;

public class TransientRenderException extends RuntimeException {
    public TransientRenderException(String message) {
        super(message);
    }
}