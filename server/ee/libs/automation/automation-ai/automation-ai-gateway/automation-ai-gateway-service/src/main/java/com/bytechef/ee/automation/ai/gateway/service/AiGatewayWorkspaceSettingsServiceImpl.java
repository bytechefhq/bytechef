/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayWorkspaceSettings;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs {@link AiGatewayWorkspaceSettings} with a single {@link Property} row per workspace, keyed by
 * {@link AiGatewayWorkspaceSettings#PROPERTY_KEY} and scoped to {@code WORKSPACE}. Reuses the platform configuration
 * store rather than introducing a dedicated table — same shape, same audit/versioning, same encryption semantics for
 * free.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayWorkspaceSettingsServiceImpl implements AiGatewayWorkspaceSettingsService {

    private static final String KEY_CACHE_ENABLED = "cacheEnabled";
    private static final String KEY_CACHE_TTL_SECONDS = "cacheTtlSeconds";
    private static final String KEY_DEFAULT_ROUTING_POLICY_ID = "defaultRoutingPolicyId";
    private static final String KEY_LOG_RETENTION_DAYS = "logRetentionDays";
    private static final String KEY_REDACT_PII = "redactPii";
    private static final String KEY_RETRY_COUNT = "retryCount";
    private static final String KEY_SOFT_BUDGET_WARNING_PCT = "softBudgetWarningPct";
    private static final String KEY_TIMEOUT_MS = "timeoutMs";

    private final PropertyService propertyService;

    AiGatewayWorkspaceSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayWorkspaceSettings> findByWorkspaceId(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return propertyService
            .fetchProperty(AiGatewayWorkspaceSettings.PROPERTY_KEY, Property.Scope.WORKSPACE, workspaceId)
            .map(property -> toSettings(workspaceId, property.getValue()));
    }

    @Override
    public AiGatewayWorkspaceSettings upsert(AiGatewayWorkspaceSettings settings) {
        Validate.notNull(settings, "settings must not be null");
        Validate.notNull(settings.workspaceId(), "settings.workspaceId must not be null");

        Map<String, Object> value = toMap(settings);

        propertyService.save(
            AiGatewayWorkspaceSettings.PROPERTY_KEY, value, Property.Scope.WORKSPACE, settings.workspaceId());

        return settings;
    }

    private static Map<String, Object> toMap(AiGatewayWorkspaceSettings settings) {
        Map<String, Object> value = new HashMap<>();

        // Only persist non-null overrides — null means "inherit from system default" and must not collide with an
        // explicit override of a different field.
        if (settings.cacheEnabled() != null) {
            value.put(KEY_CACHE_ENABLED, settings.cacheEnabled());
        }

        if (settings.cacheTtlSeconds() != null) {
            value.put(KEY_CACHE_TTL_SECONDS, settings.cacheTtlSeconds());
        }

        if (settings.defaultRoutingPolicyId() != null) {
            value.put(KEY_DEFAULT_ROUTING_POLICY_ID, settings.defaultRoutingPolicyId());
        }

        if (settings.logRetentionDays() != null) {
            value.put(KEY_LOG_RETENTION_DAYS, settings.logRetentionDays());
        }

        if (settings.retryCount() != null) {
            value.put(KEY_RETRY_COUNT, settings.retryCount());
        }

        if (settings.softBudgetWarningPct() != null) {
            value.put(KEY_SOFT_BUDGET_WARNING_PCT, settings.softBudgetWarningPct());
        }

        if (settings.timeoutMs() != null) {
            value.put(KEY_TIMEOUT_MS, settings.timeoutMs());
        }

        if (settings.redactPii() != null) {
            value.put(KEY_REDACT_PII, settings.redactPii());
        }

        return value;
    }

    private static AiGatewayWorkspaceSettings toSettings(Long workspaceId, Map<String, ?> value) {
        return new AiGatewayWorkspaceSettings(
            workspaceId,
            intValue(value, KEY_RETRY_COUNT),
            intValue(value, KEY_TIMEOUT_MS),
            (Boolean) value.get(KEY_CACHE_ENABLED),
            intValue(value, KEY_CACHE_TTL_SECONDS),
            intValue(value, KEY_LOG_RETENTION_DAYS),
            longValue(value, KEY_DEFAULT_ROUTING_POLICY_ID),
            intValue(value, KEY_SOFT_BUDGET_WARNING_PCT),
            (Boolean) value.get(KEY_REDACT_PII));
    }

    private static Integer intValue(Map<String, ?> value, String key) {
        Object raw = value.get(key);

        return raw == null ? null : ((Number) raw).intValue();
    }

    private static Long longValue(Map<String, ?> value, String key) {
        Object raw = value.get(key);

        return raw == null ? null : ((Number) raw).longValue();
    }
}
