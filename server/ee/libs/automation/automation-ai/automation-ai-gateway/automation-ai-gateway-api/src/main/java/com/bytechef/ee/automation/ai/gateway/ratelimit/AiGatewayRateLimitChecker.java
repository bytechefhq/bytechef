/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Public surface for AI Gateway rate-limit checks. Lives in the api module so the public-rest layer can call
 * pre-request control-plane and OTLP-ingest checks without depending on the service module's Redis client, in-memory
 * counter, or rate-limit-rule storage.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiGatewayRateLimitChecker {

    /**
     * Runs all applicable workspace + project rate-limit rules against the current request. Throws
     * {@link com.bytechef.ee.platform.ai.gateway.domain.RateLimitExceededException} if any limit is breached; returns
     * silently if none apply or none are enforced.
     */
    void checkRateLimits(
        long workspaceId, @Nullable Long projectId, @Nullable String userId,
        @Nullable Map<String, String> customProperties);

    /**
     * Per-workspace rate-limit for the OTLP ingest endpoint. Uses a 60-second sliding window with a configurable
     * default RPM, optionally overridable per workspace. Returns the limiter result; the caller decides whether to
     * reject or downgrade.
     */
    AiGatewayRateLimitResult checkOtlpIngest(long workspaceId);

    /**
     * Per-workspace rate-limit for control-plane endpoints (datasets, experiments, external-scores CRUD). Uses a
     * 60-second sliding window with a configurable default RPM per {@code endpointTag} for the workspace.
     */
    AiGatewayRateLimitResult checkWorkspaceRequest(long workspaceId, String endpointTag);
}
