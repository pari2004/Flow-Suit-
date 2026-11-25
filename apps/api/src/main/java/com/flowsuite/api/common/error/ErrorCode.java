package com.flowsuite.api.common.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical application error codes. Each carries a default HTTP status and a safe,
 * user-facing default message. The {@code code} name is stable and intended for clients
 * to branch on; the message may be overridden per-throw for actionable detail.
 */
public enum ErrorCode {

    // --- 400 ---
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "One or more fields are invalid."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid."),

    // --- 401 ---
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The token has expired."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The token is invalid."),

    // --- 403 ---
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),
    PLAN_LIMIT_REACHED(HttpStatus.FORBIDDEN, "Your current plan's limit has been reached."),

    // --- 404 ---
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),

    // --- 405 ---
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The HTTP method is not supported for this resource."),

    // --- 409 ---
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists."),

    // --- 429 ---
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),
    API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Your plan's monthly API request limit has been reached."),

    // --- 500 ---
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
