package com.hungpc.blog.controller;

import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.SyncAllResult;
import com.hungpc.blog.dto.response.SyncResult;
import com.hungpc.blog.service.PostSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Git sync operations
 */
@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final PostSyncService syncService;

    /**
     * Sync single post from Git repository
     * POST /api/v1/admin/sync/posts/{postId}
     * 
     * @param postId ID of post to sync
     * @return Sync result
     */
    @PostMapping("/posts/{postId}")
    public ResponseEntity<BaseResponse<SyncResult>> syncPost(@PathVariable Long postId) {
        log.info("POST /api/v1/admin/sync/posts/{} - Sync single post", postId);

        SyncResult result = syncService.syncPost(postId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(BaseResponse.success(
                    "Post synced successfully",
                    result));
        } else {
            return ResponseEntity.ok(BaseResponse.error(
                    result.getErrorMessage()
                    ));
        }
    }

    /**
     * Sync all posts from Git repository (batch operation)
     * POST /api/v1/admin/sync/posts/all
     * 
     * @return Summary of sync operation
     */
    @PostMapping("/posts/all")
    public ResponseEntity<BaseResponse<SyncAllResult>> syncAllPosts() {
        log.info("POST /api/v1/admin/sync/posts/all - Sync all posts");

        SyncAllResult result = syncService.syncAllPosts();

        String message = String.format(
                "Sync completed. Synced: %d, Skipped: %d, Errors: %d",
                result.getSynced(),
                result.getSkipped(),
                result.getErrors().size());

        return ResponseEntity.ok(BaseResponse.success(message, result));
    }
}
