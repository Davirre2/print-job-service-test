package com.adobe.printservice.web.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Generic error body used across all job endpoints. Kept as one shape here, rather than inline
 * in each handler, so future error cases (job not found, result not ready, etc.) can reuse it
 * without inventing a new format each time.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
