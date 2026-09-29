package com.zovira.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes RFC 9457 problem responses for failures raised inside the security filter chain, which
 * never reach {@code @RestControllerAdvice}.
 */
@Component
public class ProblemResponseWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED,
                "Your session has expired or is invalid. Please sign in again.");
    }

    @Override
    public void handle(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                "You do not have permission to perform this action");
    }

    public void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        ProblemDetail problem = GlobalExceptionHandler.problem(status, code, detail);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
