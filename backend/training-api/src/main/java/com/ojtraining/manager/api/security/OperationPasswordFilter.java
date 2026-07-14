package com.ojtraining.manager.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtraining.manager.api.config.OperationPasswordProperties;
import com.ojtraining.manager.api.web.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class OperationPasswordFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Operation-Password";
    private static final Set<String> PUBLIC_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Logger log = LoggerFactory.getLogger(OperationPasswordFilter.class);

    private final byte[] expectedPassword;
    private final ObjectMapper objectMapper;

    public OperationPasswordFilter(OperationPasswordProperties properties, ObjectMapper objectMapper) {
        this.expectedPassword = properties.operationPassword().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER_NAME);
        byte[] suppliedBytes = supplied == null
                ? new byte[0]
                : supplied.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedPassword, suppliedBytes)) {
            log.warn("Write request rejected, errorCode=OPERATION_PASSWORD_INVALID, method={}, path={}",
                    request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "操作密码错误"
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
