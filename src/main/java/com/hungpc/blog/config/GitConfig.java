package com.hungpc.blog.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Configuration for Git repository integration
 */
@Configuration
@ConfigurationProperties(prefix = "git.repo")
@Data
public class GitConfig {

    /**
     * Git repository URL
     * Example: https://github.com/HungPhan200591/NoteRepo.git
     */
    private String url;

    /**
     * Branch to track (default: main)
     */
    private String branch = "main";

    /**
     * Local path to clone repository
     * Default: system temp directory + /noterepo
     */
    private String localPath;

    /**
     * Path to content directory within repository
     * Configured in application.yml: git.repo.content-path
     */
    private String contentPath;

    /**
     * GitHub Personal Access Token for private repository access
     * Configured in application.yml: git.repo.token
     */
    private String token;

    @PostConstruct
    public void init() {
        // Ensure local path exists
        if (localPath != null) {
            File dir = new File(localPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    System.out.println("Created Git local directory: " + localPath);
                }
            }
        }
    }
}
