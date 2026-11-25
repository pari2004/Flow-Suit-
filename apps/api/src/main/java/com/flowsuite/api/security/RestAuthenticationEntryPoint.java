package com.flowsuite.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowsuite.api.common.error.ApiError;
import com.flowsuite.api.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a standard {@link ApiError} (401) when an unauthenticated request hits a protected route. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode code = ErrorCode.UNAUTHENTICATED;
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(code.name(), code.defaultMessage(), code.status().value());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
