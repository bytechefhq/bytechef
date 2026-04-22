/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.automation.ai.gateway.domain.RateLimitExceededException;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Checks all applicable rate limit rules for a workspace/project and throws {@link RateLimitExceededException} if any
 * limit is breached.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(
    prefix = "bytechef.ai.gateway.rate-limiting", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayRateLimitChecker {

    private static final Logger logger = LoggerFactory.getLogger(AiGatewayRateLimitChecker.class);

    private static final String API_KEY_RATE_LIMIT_KEY_PREFIX = "ai_gateway_api_key_rate_limit_";

    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiGatewayRateLimiter aiGatewayRateLimiter;
    private final AiGatewayRateLimitService aiGatewayRateLimitService;
    private final PropertyService propertyService;

    public AiGatewayRateLimitChecker(
        AiGatewayMetrics aiGatewayMetrics,
        AiGatewayRateLimiter aiGatewayRateLimiter,
        AiGatewayRateLimitService aiGatewayRateLimitService,
        PropertyService propertyService) {

        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiGatewayRateLimiter = aiGatewayRateLimiter;
        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
        this.propertyService = propertyService;
    }

    public void checkRateLimits(
        long workspaceId, @Nullable Long projectId, @Nullable String userId,
        @Nullable Map<String, String> customProperties) {

        checkApiKeyRateLimit(workspaceId);

        List<AiGatewayRateLimit> rateLimits = aiGatewayRateLimitService.getEnabledRateLimitsByWorkspaceId(workspaceId);

        for (AiGatewayRateLimit rateLimit : rateLimits) {
            if (rateLimit.getProjectId() != null && !rateLimit.getProjectId()
                .equals(projectId)) {
                continue;
            }

            String rateLimitKey = buildKey(rateLimit, workspaceId, userId, customProperties);

            if (rateLimitKey == null) {
                continue;
            }

            AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(
                rateLimitKey, rateLimit.getLimitValue(), rateLimit.getWindowSeconds());

            if (!result.allowed()) {
                logger.warn(
                    "Rate limit '{}' exceeded for workspace {} (key: {})",
                    rateLimit.getName(), workspaceId, rateLimitKey);

                aiGatewayMetrics.incrementRateLimitRejection(rateLimit.getName());

                throw new RateLimitExceededException(
                    "Rate limit '" + rateLimit.getName() + "' exceeded. " +
                        "Limit: " + rateLimit.getLimitValue() + " per " + rateLimit.getWindowSeconds() +
                        "s. Resets at: " + result.resetAtEpochMs());
            }
        }
    }

    /**
     * Per-ApiKey rate-limit override. Reads the authenticated {@link AiGatewayApiKeyAuthenticationToken} from the
     * security context; if the key has a configured RPM override stored as a workspace-scoped Property under
     * {@code ai_gateway_api_key_rate_limit_<apiKeyId>}, enforces a 60-second sliding window with that limit. Returns
     * silently when no API-key authentication is present or no override is configured — the workspace-level rules in
     * {@link #checkRateLimits} still apply.
     */
    private void checkApiKeyRateLimit(long workspaceId) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (!(authentication instanceof AiGatewayApiKeyAuthenticationToken token) || token.getApiKeyId() == null) {
            return;
        }

        Long apiKeyId = token.getApiKeyId();

        propertyService
            .fetchProperty(API_KEY_RATE_LIMIT_KEY_PREFIX + apiKeyId, Property.Scope.WORKSPACE, workspaceId)
            .ifPresent(property -> {
                Object rpmValue = property.get("rpm");

                if (!(rpmValue instanceof Number rpmNumber)) {
                    return;
                }

                int rpm = rpmNumber.intValue();

                if (rpm <= 0) {
                    return;
                }

                String key = "ai-gw-rl:ws:" + workspaceId + ":apikey:" + apiKeyId;

                AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(key, rpm, 60);

                if (!result.allowed()) {
                    logger.warn(
                        "Per-ApiKey rate limit exceeded for apiKeyId {} in workspace {}: {} rpm",
                        apiKeyId, workspaceId, rpm);

                    aiGatewayMetrics.incrementRateLimitRejection("api_key_rpm");

                    throw new RateLimitExceededException(
                        "API key rate limit exceeded. Limit: " + rpm + " per 60s. Resets at: " +
                            result.resetAtEpochMs());
                }
            });
    }

    private String buildKey(
        AiGatewayRateLimit rateLimit, long workspaceId, @Nullable String userId,
        @Nullable Map<String, String> customProperties) {

        String baseKey = "ai-gw-rl:" + workspaceId + ":" + rateLimit.getId();

        return switch (rateLimit.getScope()) {
            case GLOBAL -> baseKey + ":global";
            case PER_USER -> {
                if (userId == null) {
                    yield null;
                }

                yield baseKey + ":user:" + userId;
            }
            case PER_PROPERTY -> {
                String propertyKey = rateLimit.getPropertyKey();

                if (propertyKey == null || customProperties == null || !customProperties.containsKey(propertyKey)) {
                    yield null;
                }

                yield baseKey + ":prop:" + propertyKey + ":" + customProperties.get(propertyKey);
            }
        };
    }
}
