package com.hungpc.blog.controller;

import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.StatsResponse;
import com.hungpc.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final PostService postService;

    /**
     * Get dashboard statistics
     * GET /api/v1/admin/stats
     */
    @GetMapping
    public ResponseEntity<BaseResponse<StatsResponse>> getStats() {
        log.info("GET /api/v1/admin/stats");
        StatsResponse stats = postService.getStats();
        return ResponseEntity.ok(BaseResponse.success(stats));
    }
}
