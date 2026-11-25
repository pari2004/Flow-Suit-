package com.flowsuite.api.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates exceptions into the standard {@link ApiError} body so every error response —
 * whether a thrown {@link ApiException}, a bean-validation failure, a Spring Security
 * denial, or an unexpected error — has the same {@code {code, message, status}} shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        return build(code, ex.getMessage(), null);
    }

    /** Bean validation on {@code @Valid @RequestBody} DTOs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.BAD_REQUEST, "Malformed request body.", null);
    }

    /** Unsupported HTTP method on an otherwise-mapped path (e.g. PATCH where only PUT exists). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.defaultMessage(), null);
    }

    /** Thrown by method security (@PreAuthorize) when a role check fails. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log the detail server-side; never leak internals to the client.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null);
    }

    private static ResponseEntity<ApiError> build(ErrorCode code, String message, List<ApiError.FieldError> fieldErrors) {
        HttpStatus status = code.status();
        ApiError body = new ApiError(code.name(), message, status.value(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private static ApiError.FieldError toFieldError(FieldError fe) {
        return new ApiError.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
