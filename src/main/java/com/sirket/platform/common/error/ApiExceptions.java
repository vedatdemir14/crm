package com.sirket.platform.common.error;

import org.springframework.http.HttpStatus;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class ApiException extends RuntimeException {
        private final HttpStatus status;
        private final String errorCode;

        public ApiException(HttpStatus status, String errorCode, String message) {
            super(message);
            this.status = status;
            this.errorCode = errorCode;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class NotFound extends ApiException {
        public NotFound(String message) {
            super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
        }
    }

    public static class Conflict extends ApiException {
        public Conflict(String message) {
            super(HttpStatus.CONFLICT, "CONFLICT", message);
        }
    }

    public static class Unauthorized extends ApiException {
        public Unauthorized(String message) {
            super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
        }
    }
}
