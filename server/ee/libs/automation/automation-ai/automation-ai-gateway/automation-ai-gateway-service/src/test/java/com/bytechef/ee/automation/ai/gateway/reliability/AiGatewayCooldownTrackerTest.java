/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayCooldownTrackerTest {

    private AiGatewayCooldownTracker aiGatewayCooldownTracker;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager(
            "ai-gateway-cooldown", "ai-gateway-failure-count");

        aiGatewayCooldownTracker = new AiGatewayCooldownTracker(cacheManager);
    }

    @Test
    void testIsCooledDownReturnsFalseWhenNotCooledDown() {
        assertFalse(aiGatewayCooldownTracker.isCooledDown(1L));
    }

    @Test
    void testRecordFailureBelowThreshold() {
        aiGatewayCooldownTracker.recordFailure(1L);
        aiGatewayCooldownTracker.recordFailure(1L);

        assertFalse(aiGatewayCooldownTracker.isCooledDown(1L));
    }

    @Test
    void testRecordFailureAtThreshold() {
        aiGatewayCooldownTracker.recordFailure(1L);
        aiGatewayCooldownTracker.recordFailure(1L);
        aiGatewayCooldownTracker.recordFailure(1L);

        assertTrue(aiGatewayCooldownTracker.isCooledDown(1L));
    }

    @Test
    void testRecordSuccessClearsCooldown() {
        aiGatewayCooldownTracker.recordFailure(1L);
        aiGatewayCooldownTracker.recordFailure(1L);
        aiGatewayCooldownTracker.recordFailure(1L);

        assertTrue(aiGatewayCooldownTracker.isCooledDown(1L));

        aiGatewayCooldownTracker.recordSuccess(1L);

        assertFalse(aiGatewayCooldownTracker.isCooledDown(1L));
    }

    @Test
    void testThrowsOnMissingCooldownCacheAtFirstUse(@Mock CacheManager mockCacheManager) {
        when(mockCacheManager.getCache("ai-gateway-cooldown")).thenReturn(null);

        AiGatewayCooldownTracker tracker = new AiGatewayCooldownTracker(mockCacheManager);

        assertThrows(
            IllegalStateException.class,
            () -> tracker.isCooledDown(1L));
    }

    @Test
    void testThrowsOnMissingFailureCountCacheAtFirstUse(@Mock CacheManager mockCacheManager) {
        when(mockCacheManager.getCache("ai-gateway-failure-count")).thenReturn(null);

        AiGatewayCooldownTracker tracker = new AiGatewayCooldownTracker(mockCacheManager);

        assertThrows(
            IllegalStateException.class,
            () -> tracker.recordFailure(1L));
    }
}
