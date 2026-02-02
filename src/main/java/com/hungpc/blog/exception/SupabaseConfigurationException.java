package com.hungpc.blog.exception;

public class SupabaseConfigurationException extends RuntimeException {
    public SupabaseConfigurationException(String message) {
        super(message);
    }

    public SupabaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
