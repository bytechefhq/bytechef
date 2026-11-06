/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.ee.platform.ai.gateway.exception.AiGatewayEmbeddedSettingsConflictException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayEmbeddedSettingsServiceImpl implements AiGatewayEmbeddedSettingsService {

    private static final String KEY_CACHE_ENABLED = "cacheEnabled";
    private static final String KEY_CACHE_TTL_SECONDS = "cacheTtlSeconds";
    private static final String KEY_DEFAULT_CONNECTED_USER_BUDGET_CAP = "defaultConnectedUserBudgetCap";
    private static final String KEY_DEFAULT_ROUTING_POLICY_ID = "defaultRoutingPolicyId";
    private static final String KEY_LOG_RETENTION_DAYS = "logRetentionDays";
    private static final String KEY_RETRY_COUNT = "retryCount";
    private static final String KEY_SOFT_BUDGET_WARNING_PCT = "softBudgetWarningPct";
    private static final String KEY_TIMEOUT_MS = "timeoutMs";

    private final PropertyService propertyService;

    AiGatewayEmbeddedSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayEmbeddedSettings> find(long environmentId) {
        return propertyService
            .fetchProperty(AiGatewayEmbeddedSettings.PROPERTY_KEY, Property.Scope.EMBEDDED, null, environmentId)
            .map(property -> toSettings(environmentId, property.getValue()));
    }

    @Override
    public AiGatewayEmbeddedSettings upsert(AiGatewayEmbeddedSettings settings) {
        Validate.notNull(settings, "settings must not be null");
        Validate.notNull(settings.environmentId(), "settings.environmentId must not be null");

        Map<String, Object> value = toMap(settings);

        try {
            propertyService.save(
                AiGatewayEmbeddedSettings.PROPERTY_KEY, value, Property.Scope.EMBEDDED, null,
                settings.environmentId());
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw new AiGatewayEmbeddedSettingsConflictException(
                "Embedded AI Gateway settings for environment " + settings.environmentId()
                    + " were concurrently created by another writer",
                dataIntegrityViolationException);
        }

        return settings;
    }

    private static Map<String, Object> toMap(AiGatewayEmbeddedSettings settings) {
        Map<String, Object> value = new HashMap<>();

        if (settings.cacheEnabled() != null) {
            value.put(KEY_CACHE_ENABLED, settings.cacheEnabled());
        }

        if (settings.cacheTtlSeconds() != null) {
            value.put(KEY_CACHE_TTL_SECONDS, settings.cacheTtlSeconds());
        }

        if (settings.defaultConnectedUserBudgetCap() != null) {
            value.put(KEY_DEFAULT_CONNECTED_USER_BUDGET_CAP, settings.defaultConnectedUserBudgetCap());
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

        return value;
    }

    private static AiGatewayEmbeddedSettings toSettings(long environmentId, Map<String, ?> value) {
        return new AiGatewayEmbeddedSettings(
            environmentId,
            intValue(value, KEY_RETRY_COUNT),
            intValue(value, KEY_TIMEOUT_MS),
            (Boolean) value.get(KEY_CACHE_ENABLED),
            intValue(value, KEY_CACHE_TTL_SECONDS),
            intValue(value, KEY_LOG_RETENTION_DAYS),
            longValue(value, KEY_DEFAULT_ROUTING_POLICY_ID),
            intValue(value, KEY_SOFT_BUDGET_WARNING_PCT),
            bigDecimalValue(value, KEY_DEFAULT_CONNECTED_USER_BUDGET_CAP));
    }

    private static Integer intValue(Map<String, ?> value, String key) {
        Object raw = value.get(key);

        return raw == null ? null : ((Number) raw).intValue();
    }

    private static Long longValue(Map<String, ?> value, String key) {
        Object raw = value.get(key);

        return raw == null ? null : ((Number) raw).longValue();
    }

    private static BigDecimal bigDecimalValue(Map<String, ?> value, String key) {
        Object raw = value.get(key);

        return raw == null ? null : new BigDecimal(raw.toString());
    }
}
