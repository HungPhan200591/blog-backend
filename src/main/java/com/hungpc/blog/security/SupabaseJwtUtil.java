package com.hungpc.blog.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hungpc.blog.config.SupabaseProperties;
import com.hungpc.blog.exception.SupabaseAuthException;
import com.hungpc.blog.exception.SupabaseConfigurationException;
import com.hungpc.blog.exception.SupabaseTokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

@Component
@Slf4j
public class SupabaseJwtUtil {
    
    private final JwkProvider jwkProvider;
    private final String issuer;
    
    public SupabaseJwtUtil(SupabaseProperties properties) {
        this.issuer = properties.getUrl() + "/auth/v1";
        log.info("Initialized SupabaseJwtUtil with issuer: {}", this.issuer);
        try {
            this.jwkProvider = new UrlJwkProvider(new URL(this.issuer + "/.well-known/jwks.json"));
        } catch (Exception e) {
            log.error("Failed to initialize JWK Provider for: {}", this.issuer, e);
            throw new SupabaseConfigurationException("Failed to initialize Supabase JWK Provider", e);
        }
    }
    
    /**
     * Verify and decode Supabase JWT token supporting modern Asymmetric Signing Keys
     */
    public DecodedJWT verifyToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            
            // Get Public Key from Supabase dynamically using the 'kid' (Key ID)
            Jwk jwk = jwkProvider.get(jwt.getKeyId());
            Algorithm algorithm;
            
            if ("ES256".equals(jwk.getAlgorithm())) {
                algorithm = Algorithm.ECDSA256((ECPublicKey) jwk.getPublicKey(), null);
            } else {
                log.error("Unsupported algorithm in JWK: {}", jwk.getAlgorithm());
                throw new SupabaseAuthException("Unsupported encryption algorithm: " + jwk.getAlgorithm());
            }

            // Verify the token
            JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token);
            
            if (jwt.getExpiresAt().before(new Date())) {
                throw new SupabaseTokenExpiredException("Token expired at " + jwt.getExpiresAt());
            }
            
            return jwt;
        } catch (SupabaseAuthException e) {
            log.error("Auth Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT Verification Unexpected Error: {} [Token Subject: {}]", e.getMessage(), 
                token.split("\\.").length > 1 ? JWT.decode(token).getSubject() : "unknown");
            throw new SupabaseAuthException("Invalid token: " + e.getMessage(), e);
        }
    }
    
    public String getUserId(String token) {
        return verifyToken(token).getSubject();
    }
    
    public String getUserEmail(String token) {
        return verifyToken(token).getClaim("email").asString();
    }
}
