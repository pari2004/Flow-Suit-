package com.flowsuite.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowsuite.api.common.error.ApiError;
import com.flowsuite.api.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a standard {@link ApiError} (403) when an authenticated user lacks the required role. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode code = ErrorCode.FORBIDDEN;
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(code.name(), code.defaultMessage(), code.status().value());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
