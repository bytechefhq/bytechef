/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayCooldownTracker {

    private static final String COOLDOWN_CACHE = "ai-gateway-cooldown";
    private static final int DEFAULT_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final String FAILURE_COUNT_CACHE = "ai-gateway-failure-count";
    private static final Logger logger = LoggerFactory.getLogger(AiGatewayCooldownTracker.class);

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI")
    public AiGatewayCooldownTracker(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public boolean isCooledDown(long deploymentId) {
        Instant until = getCooldownCache().get(deploymentId, Instant.class);

        if (until == null) {
            return false;
        }

        if (Instant.now()
            .isAfter(until)) {

            getCooldownCache().evict(deploymentId);
            getFailureCountCache().evict(deploymentId);

            return false;
        }

        return true;
    }

    public void recordFailure(long deploymentId) {
        Integer currentCount = getFailureCountCache().get(deploymentId, Integer.class);
        int count = (currentCount != null ? currentCount : 0) + 1;

        getFailureCountCache().put(deploymentId, count);

        if (count >= DEFAULT_FAILURE_THRESHOLD) {
            logger.warn("Deployment {} entering cooldown for {}s after {} consecutive failures",
                deploymentId, DEFAULT_COOLDOWN_SECONDS, count);

            getCooldownCache().put(deploymentId, Instant.now()
                .plusSeconds(DEFAULT_COOLDOWN_SECONDS));
        }
    }

    public void recordSuccess(long deploymentId) {
        getFailureCountCache().evict(deploymentId);
        getCooldownCache().evict(deploymentId);
    }

    private Cache getCooldownCache() {
        Cache cache = cacheManager.getCache(COOLDOWN_CACHE);

        if (cache == null) {
            throw new IllegalStateException(
                "Required cache '" + COOLDOWN_CACHE + "' is not configured in the CacheManager");
        }

        return cache;
    }

    private Cache getFailureCountCache() {
        Cache cache = cacheManager.getCache(FAILURE_COUNT_CACHE);

        if (cache == null) {
            throw new IllegalStateException(
                "Required cache '" + FAILURE_COUNT_CACHE + "' is not configured in the CacheManager");
        }

        return cache;
    }
}
