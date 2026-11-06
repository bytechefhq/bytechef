/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayRateLimitService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.domain.RateLimitExceededException;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.ratelimit.AiGatewayRateLimiter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(
    prefix = "bytechef.ai.gateway.rate-limiting", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayRateLimitCheckerImpl implements AiGatewayRateLimitChecker {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayRateLimitCheckerImpl.class);

    private static final String API_KEY_RATE_LIMIT_KEY_PREFIX = "ai_gateway_api_key_rate_limit_";
    private static final String OTLP_RATE_LIMIT_PROPERTY_KEY = "ai_gateway_otlp_rate_limit";
    private static final int OTLP_DEFAULT_RPM = 10_000;
    private static final int OTLP_WINDOW_SECONDS = 60;

    private static final String CONTROL_PLANE_RATE_LIMIT_PROPERTY_KEY = "ai_gateway_control_plane_rate_limit";
    private static final int CONTROL_PLANE_DEFAULT_RPM = 600;
    private static final int CONTROL_PLANE_WINDOW_SECONDS = 60;

    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiGatewayRateLimiter aiGatewayRateLimiter;
    private final WorkspaceAiGatewayRateLimitService workspaceAiGatewayRateLimitService;
    private final PropertyService propertyService;

    public AiGatewayRateLimitCheckerImpl(
        AiGatewayMetrics aiGatewayMetrics,
        AiGatewayRateLimiter aiGatewayRateLimiter,
        WorkspaceAiGatewayRateLimitService workspaceAiGatewayRateLimitService,
        PropertyService propertyService) {
        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiGatewayRateLimiter = aiGatewayRateLimiter;
        this.workspaceAiGatewayRateLimitService = workspaceAiGatewayRateLimitService;
        this.propertyService = propertyService;
    }

    @Override
    public void checkRateLimits(
        long workspaceId, @Nullable Long projectId, @Nullable String userId,
        @Nullable Map<String, String> customProperties) {
        checkApiKeyRateLimit(workspaceId);

        List<AiGatewayRateLimit> rateLimits =
            workspaceAiGatewayRateLimitService.getEnabledRateLimitsByWorkspaceId(workspaceId);

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
                log.warn(
                    "Rate limit '{}' exceeded for workspace {} (key: {})",
                    rateLimit.getName(), workspaceId, rateLimitKey);

                aiGatewayMetrics.incrementRateLimitRejection(rateLimit.getName());

                throw new RateLimitExceededException(
                    "Rate limit '" + rateLimit.getName() + "' exceeded. " +
                        "Limit: " + rateLimit.getLimitValue() + " per " + rateLimit.getWindowSeconds() +
                        "s. Resets at: " + result.resetAtEpochMs(),
                    result.resetAtEpochMs());
            }
        }
    }

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
                    log.warn(
                        "Per-ApiKey rate-limit property for apiKeyId {} in workspace {} has non-numeric rpm field {}" +
                            " ({}); skipping override",
                        apiKeyId, workspaceId, rpmValue, rpmValue == null ? "null"
                            : rpmValue.getClass()
                                .getSimpleName());

                    return;
                }

                int rpm = rpmNumber.intValue();

                if (rpm <= 0) {
                    log.warn(
                        "Per-ApiKey rate-limit property for apiKeyId {} in workspace {} has non-positive rpm field" +
                            " {}; skipping override",
                        apiKeyId, workspaceId, rpm);

                    return;
                }

                String key = "ai-gw-rl:ws:" + workspaceId + ":apikey:" + apiKeyId;

                AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(key, rpm, 60);

                if (!result.allowed()) {
                    log.warn(
                        "Per-ApiKey rate limit exceeded for apiKeyId {} in workspace {}: {} rpm",
                        apiKeyId, workspaceId, rpm);

                    aiGatewayMetrics.incrementRateLimitRejection("api_key_rpm");

                    throw new RateLimitExceededException(
                        "API key rate limit exceeded. Limit: " + rpm + " per 60s. Resets at: " +
                            result.resetAtEpochMs(),
                        result.resetAtEpochMs());
                }
            });
    }

    @Override
    public AiGatewayRateLimitResult checkOtlpIngest(long workspaceId) {
        int rpm = readRpmOverride(OTLP_RATE_LIMIT_PROPERTY_KEY, workspaceId, OTLP_DEFAULT_RPM);

        String key = "ai-gw-rl:otlp:workspace:" + workspaceId;

        AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(key, rpm, OTLP_WINDOW_SECONDS);

        if (!result.allowed()) {
            log.warn("OTLP ingest rate limit exceeded for workspace {}: {} rpm", workspaceId, rpm);

            aiGatewayMetrics.incrementRateLimitRejection("otlp_ingest");
        }

        return result;
    }

    @Override
    public AiGatewayRateLimitResult checkWorkspaceRequest(long workspaceId, String endpointTag) {
        int rpm = readRpmOverride(CONTROL_PLANE_RATE_LIMIT_PROPERTY_KEY, workspaceId, CONTROL_PLANE_DEFAULT_RPM);

        String key = "ai-gw-rl:" + endpointTag + ":workspace:" + workspaceId;

        AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(key, rpm, CONTROL_PLANE_WINDOW_SECONDS);

        if (!result.allowed()) {
            log.warn(
                "Control-plane rate limit exceeded for workspace {} endpoint {}: {} rpm", workspaceId, endpointTag,
                rpm);

            aiGatewayMetrics.incrementRateLimitRejection(endpointTag);
        }

        return result;
    }

    private int readRpmOverride(String propertyKey, long workspaceId, int defaultRpm) {
        return propertyService
            .fetchProperty(propertyKey, Property.Scope.WORKSPACE, workspaceId)
            .map(property -> property.get("rpm"))
            .map(value -> {
                if (!(value instanceof Number numericValue)) {
                    log.warn(
                        "Rate-limit property {} for workspace {} has non-numeric rpm field {} ({}); using default {}",
                        propertyKey, workspaceId, value, value.getClass()
                            .getSimpleName(),
                        defaultRpm);

                    return defaultRpm;
                }

                int rpm = numericValue.intValue();

                if (rpm <= 0) {
                    log.warn(
                        "Rate-limit property {} for workspace {} has non-positive rpm field {}; using default {}",
                        propertyKey, workspaceId, rpm, defaultRpm);

                    return defaultRpm;
                }

                return rpm;
            })
            .orElse(defaultRpm);
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
