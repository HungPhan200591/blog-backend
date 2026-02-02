package com.hungpc.blog.exception;

public class SupabaseTokenExpiredException extends SupabaseAuthException {
    public SupabaseTokenExpiredException(String message) {
        super(message);
    }
}
