package com.hungpc.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for sync operation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    private Long postId;
    private String slug;
    private boolean success;
    private LocalDateTime syncedAt;
    private Integer contentLength;
    private String errorMessage;
}
