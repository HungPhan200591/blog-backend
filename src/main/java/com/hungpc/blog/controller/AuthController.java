package com.hungpc.blog.controller;

import com.hungpc.blog.security.SupabaseJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SupabaseJwtUtil jwtUtil;
    
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(HttpServletRequest request, Authentication authentication) {
        log.info("Current user:");
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        
        return Map.of(
            "id", jwtUtil.getUserId(token),
            "email", jwtUtil.getUserEmail(token),
            "principal", authentication.getPrincipal(),
            "authenticated", authentication.isAuthenticated()
        );
    }
}
