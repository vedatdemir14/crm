package com.sirket.platform.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope defined in API Endpoint Tasarımı §1.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError validation(String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), 400, "VALIDATION_ERROR", message, path, fieldErrors);
    }
}
