package br.ia.sina.api.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),
    PAIRING_CODE_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAIRING_NOT_FOUND(HttpStatus.NOT_FOUND),
    ALREADY_PAIRED(HttpStatus.CONFLICT),
    PAIRING_CODE_EXPIRED(HttpStatus.GONE),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
