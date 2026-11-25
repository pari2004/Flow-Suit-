package com.flowsuite.api.common.error;

/**
 * The single application exception type. Carries an {@link ErrorCode} (which supplies the
 * HTTP status) and an optional overriding message. Thrown anywhere in the service layer
 * and rendered to the standard {@code {code, message, status}} body by
 * {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    // --- Convenience factories for the most common cases ---

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, message);
    }

    public static ApiException conflict(ErrorCode code, String message) {
        return new ApiException(code, message);
    }
}
