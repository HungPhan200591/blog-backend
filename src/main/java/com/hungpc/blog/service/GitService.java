package com.hungpc.blog.service;

import com.hungpc.blog.config.GitConfig;
import com.hungpc.blog.dto.FileWithMetadata;
import com.hungpc.blog.dto.FrontmatterDTO;
import com.hungpc.blog.exception.GitOperationException;
import com.hungpc.blog.util.FrontmatterParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for Git repository operations
 * Handles cloning, pulling, and reading files from NoteRepo
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {

    private final GitConfig gitConfig;
    private Git git;

    /**
     * Initialize Git repository on application startup
     * Clones repo if not exists, otherwise opens existing
     */
    @PostConstruct
    public void initialize() {
        try {
            File localRepo = new File(gitConfig.getLocalPath());

            if (localRepo.exists() && new File(localRepo, ".git").exists()) {
                // Open existing repo
                git = Git.open(localRepo);
                log.info("Opened existing Git repository at {}", localRepo.getAbsolutePath());
            } else {
                // Clone repo
                cloneRepository();
            }

            // Initial pull to get latest changes
            pullLatest();

        } catch (Exception e) {
            log.error("Failed to initialize Git repository", e);
            throw new GitOperationException("Git initialization failed", e);
        }
    }

    /**
     * Clone repository from GitHub
     */
    private void cloneRepository() throws GitAPIException {
        log.info("Cloning repository from {} to {}", gitConfig.getUrl(), gitConfig.getLocalPath());

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gitConfig.getUrl())
                .setDirectory(new File(gitConfig.getLocalPath()))
                .setBranch(gitConfig.getBranch());

        // Add credentials if token provided
        String token = gitConfig.getToken();
        if (token != null && !token.isBlank()) {
            cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(token, ""));
            log.debug("Using Git token for authentication");
        }

        git = cloneCommand.call();
        log.info("Repository cloned successfully");
    }

    /**
     * Pull latest changes from remote
     * Thread-safe operation
     */
    public synchronized void pullLatest() {
        try {
            log.info("Pulling latest changes from branch: {}", gitConfig.getBranch());

            PullCommand pullCommand = git.pull()
                    .setRemoteBranchName(gitConfig.getBranch());

            // Add credentials
            String token = gitConfig.getToken();
            if (token != null && !token.isBlank()) {
                pullCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(token, ""));
            }

            PullResult result = pullCommand.call();

            if (result.isSuccessful()) {
                log.info("Pull successful. Fetched updates: {}",
                        result.getFetchResult().getTrackingRefUpdates().size());
            } else {
                log.warn("Pull completed with issues: {}", result.getMergeResult());
            }

        } catch (Exception e) {
            log.error("Failed to pull latest changes", e);
            throw new GitOperationException("Git pull failed", e);
        }
    }

    /**
     * Read file content from repository
     * 
     * @param relativePath Path relative to repo root (e.g.,
     *                     "content/post.md")
     * @return File content as String, or empty if not found
     */
    public Optional<String> readFile(String relativePath) {
        try {
            File file = new File(gitConfig.getLocalPath(), relativePath);

            if (!file.exists() || !file.isFile()) {
                log.warn("File not found: {}", relativePath);
                return Optional.empty();
            }

            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            log.debug("Read file: {} ({} bytes)", relativePath, content.length());

            return Optional.of(content);

        } catch (IOException e) {
            log.error("Failed to read file: {}", relativePath, e);
            return Optional.empty();
        }
    }

    /**
     * Find markdown file by slug
     * Tries both .md and .mdx extensions
     * 
     * @param slug Post slug (without extension)
     * @return File content if found
     */
    public Optional<String> findMarkdownFile(String slug) {
        String basePath = gitConfig.getContentPath() + slug;

        // Try .md first
        Optional<String> content = readFile(basePath + ".md");
        if (content.isPresent()) {
            log.debug("Found markdown file: {}.md", slug);
            return content;
        }

        // Try .mdx
        content = readFile(basePath + ".mdx");
        if (content.isPresent()) {
            log.debug("Found markdown file: {}.mdx", slug);
        }

        return content;
    }

    /**
     * List all markdown files in content directory
     * 
     * @return List of slugs (filenames without extensions)
     */
    public List<String> listMarkdownFiles() {
        try {
            File contentDir = new File(gitConfig.getLocalPath(), gitConfig.getContentPath());

            if (!contentDir.exists() || !contentDir.isDirectory()) {
                log.warn("Content directory not found: {}", contentDir.getAbsolutePath());
                return Collections.emptyList();
            }

            File[] files = contentDir.listFiles((dir, name) -> name.endsWith(".md") || name.endsWith(".mdx"));

            if (files == null) {
                return Collections.emptyList();
            }

            List<String> slugs = Arrays.stream(files)
                    .map(File::getName)
                    .map(name -> name.replaceAll("\\.(md|mdx)$", "")) // Remove extension
                    .toList();

            log.info("Found {} markdown files in {}", slugs.size(), contentDir.getAbsolutePath());
            return slugs;

        } catch (Exception e) {
            log.error("Failed to list markdown files", e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if file exists in Git repository
     */
    public boolean fileExists(String filePath) {
        return readFile(filePath).isPresent();
    }

    /**
     * Write file to Git repository
     * 
     * @param relativePath Path relative to repo root
     * @param content      File content
     */
    public void writeFile(String relativePath, String content) throws IOException {
        File file = new File(gitConfig.getLocalPath(), relativePath);

        // Create parent directories if needed
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            log.debug("Created directories: {}", parentDir.getAbsolutePath());
        }

        // Write file
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        log.info("✅ Wrote file: {} ({} bytes)", relativePath, content.length());
    }

    /**
     * Commit and push changes to remote
     * 
     * @param message Commit message
     * @return Commit hash (SHA-1)
     */
    public synchronized String commitAndPush(String message) throws GitAPIException {
        log.info("📝 Committing changes: {}", message);

        // Stage all changes
        git.add().addFilepattern(".").call();
        log.debug("Staged all changes");

        // Commit
        var commit = git.commit()
                .setMessage(message)
                .setAuthor("Blog System", "system@blog.com")
                .call();

        String commitHash = commit.getName();
        log.info("✅ Committed: {} ({})", commitHash.substring(0, 7), message.split("\n")[0]);

        // Push to remote
        String token = gitConfig.getToken();
        if (token != null && !token.isBlank()) {
            log.debug("Pushing to remote...");
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();
            log.info("✅ Pushed to remote");
        } else {
            log.warn("⚠️ GIT_TOKEN not set - skipping push");
        }

        return commitHash;
    }

    /**
     * Get latest commit hash
     * 
     * @return Latest commit SHA-1 hash
     */
    public String getLatestCommitHash() {
        try {
            var head = git.getRepository().resolve("HEAD");
            return head != null ? head.getName() : null;
        } catch (Exception e) {
            log.error("Failed to get latest commit hash", e);
            return null;
        }
    }

    /**
     * Find markdown file by slug and parse frontmatter
     * 
     * @param slug Post slug (without extension)
     * @return FileWithMetadata containing parsed frontmatter and content
     */
    public Optional<FileWithMetadata> findMarkdownFileWithMetadata(String slug) {
        Optional<String> rawContent = findMarkdownFile(slug);

        if (rawContent.isEmpty()) {
            log.warn("Markdown file not found for slug: {}", slug);
            return Optional.empty();
        }

        return Optional.of(parseFileWithMetadata(rawContent.get()));
    }

    /**
     * Parse frontmatter from raw markdown content
     * 
     * @param rawContent Full markdown content (with or without frontmatter)
     * @return FileWithMetadata
     */
    public FileWithMetadata parseFileWithMetadata(String rawContent) {
        FrontmatterDTO metadata = null;
        String content = rawContent;

        // Check if file has frontmatter
        if (FrontmatterParser.hasFrontmatter(rawContent)) {
            // Parse frontmatter
            metadata = FrontmatterParser.parse(rawContent);

            // Strip frontmatter from content
            content = FrontmatterParser.stripFrontmatter(rawContent);

            log.debug("Parsed frontmatter: title={}, category={}",
                    metadata != null ? metadata.getTitle() : null,
                    metadata != null ? metadata.getCategory() : null);
        } else {
            log.debug("No frontmatter found in file");
        }

        return FileWithMetadata.builder()
                .metadata(metadata)
                .content(content)
                .rawContent(rawContent)
                .build();
    }

    /**
     * Cleanup on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (git != null) {
            git.close();
            log.info("Git repository closed");
        }
    }
}
