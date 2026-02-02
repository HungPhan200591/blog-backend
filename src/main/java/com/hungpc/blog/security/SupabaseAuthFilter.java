package com.hungpc.blog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungpc.blog.exception.SupabaseAuthException;
import com.hungpc.blog.exception.SupabaseTokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseAuthFilter extends OncePerRequestFilter {

    private final SupabaseJwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filter for public endpoints
        return path.startsWith("/api/v1/posts") ||
                path.startsWith("/api/v1/meta") ||
                path.startsWith("/actuator/"); // Health checks & monitoring
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Found Bearer token, verifying...");

            try {
                String email = jwtUtil.getUserEmail(token);
                log.info("Successfully authenticated user: {}", email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                        null, new ArrayList<>());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (SupabaseTokenExpiredException e) {
                log.warn("Token expired: {}", e.getMessage());
                handleException(response, "TOKEN_EXPIRED", e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (SupabaseAuthException e) {
                log.error("Authentication failed: {}", e.getMessage());
                handleException(response, "AUTH_ERROR", e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (Exception e) {
                log.error("Unexpected security error: {}", e.getMessage());
                handleException(response, "SECURITY_ERROR", "An unexpected error occurred during authentication",
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        } else {
            log.warn("No Bearer token found in request to {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private void handleException(HttpServletResponse response, String code, String message, int status)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", code);
        errorDetails.put("message", message);
        errorDetails.put("status", status);
        errorDetails.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}
