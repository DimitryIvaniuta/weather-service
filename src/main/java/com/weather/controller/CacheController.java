package com.weather.controller;

import com.weather.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes endpoints to inspect and purge application caches.
 */
@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * GET /cache/l1
     * @return all entries in every L1 (Caffeine) cache
     */
    @GetMapping("/l1")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, Object>>> getL1() {
        return ResponseEntity.ok(cacheService.getL1CacheContents());
    }

    /**
     * GET /cache/l2
     * @return all entries in every L2 (Redis) cache
     */
    @GetMapping("/l2")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, String>>> getL2() {
        return ResponseEntity.ok(cacheService.getL2CacheContents());
    }

    /**
     * POST /cache/purge
     * Clears **all** caches.
     * <p>Only ADMIN may call.</p>
     */
    @PostMapping("/purge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> purge() {
        cacheService.purgeAllCaches();
        return ResponseEntity.noContent().build();
    }
}
