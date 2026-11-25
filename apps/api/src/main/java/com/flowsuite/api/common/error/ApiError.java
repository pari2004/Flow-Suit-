package com.flowsuite.api.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard error response body: {@code { code, message, status }} (NFR). For validation
 * failures an optional {@code fieldErrors} array is included; it is omitted otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        int status,
        List<FieldError> fieldErrors
) {
    public ApiError(String code, String message, int status) {
        this(code, message, status, null);
    }

    public record FieldError(String field, String message) {
    }
}
