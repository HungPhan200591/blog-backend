package com.hungpc.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseProperties {
    private String url;
    private String anonKey;
    private String serviceRoleKey;
}
