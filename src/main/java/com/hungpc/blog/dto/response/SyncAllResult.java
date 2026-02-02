package com.hungpc.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for batch sync operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncAllResult {

    private int totalScanned;
    private int synced;
    private int skipped;
    private List<String> errors;
    private LocalDateTime syncedAt;
}
