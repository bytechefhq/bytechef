/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
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
 * Backs {@link AiGatewayProjectSettings} with a single {@link Property} row per project, keyed by
 * {@link AiGatewayProjectSettings#PROPERTY_KEY} and scoped to {@code PROJECT}. Reuses the platform configuration store
 * rather than introducing a dedicated table — same shape, same audit/versioning, same encryption semantics for free, as
 * the workspace-settings service does for its scope.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayProjectSettingsServiceImpl implements AiGatewayProjectSettingsService {

    private static final String KEY_BLOCKED_TERMS = "blockedTerms";
    private static final String KEY_INJECTION_DETECTION_ENABLED = "injectionDetectionEnabled";
    private static final String KEY_MODERATION_ENABLED = "moderationEnabled";
    private static final String KEY_REDACT_PII = "redactPii";
    private static final String KEY_REDACT_SECRETS = "redactSecrets";
    private static final String KEY_SCAN_RESPONSES = "scanResponses";

    private final PropertyService propertyService;

    AiGatewayProjectSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayProjectSettings> findByProjectId(Long projectId) {
        Validate.notNull(projectId, "projectId must not be null");

        return propertyService
            .fetchProperty(AiGatewayProjectSettings.PROPERTY_KEY, Property.Scope.PROJECT, projectId)
            .map(property -> toSettings(projectId, property.getValue()));
    }

    @Override
    public AiGatewayProjectSettings upsert(AiGatewayProjectSettings settings) {
        Validate.notNull(settings, "settings must not be null");
        Validate.notNull(settings.projectId(), "settings.projectId must not be null");

        Map<String, Object> value = toMap(settings);

        propertyService.save(
            AiGatewayProjectSettings.PROPERTY_KEY, value, Property.Scope.PROJECT, settings.projectId());

        return settings;
    }

    private static Map<String, Object> toMap(AiGatewayProjectSettings settings) {
        Map<String, Object> value = new HashMap<>();

        // Only persist non-null overrides — null means "inherit from the workspace/global policy".
        if (settings.redactPii() != null) {
            value.put(KEY_REDACT_PII, settings.redactPii());
        }

        if (settings.redactSecrets() != null) {
            value.put(KEY_REDACT_SECRETS, settings.redactSecrets());
        }

        if (settings.blockedTerms() != null) {
            value.put(KEY_BLOCKED_TERMS, settings.blockedTerms());
        }

        if (settings.moderationEnabled() != null) {
            value.put(KEY_MODERATION_ENABLED, settings.moderationEnabled());
        }

        if (settings.injectionDetectionEnabled() != null) {
            value.put(KEY_INJECTION_DETECTION_ENABLED, settings.injectionDetectionEnabled());
        }

        if (settings.scanResponses() != null) {
            value.put(KEY_SCAN_RESPONSES, settings.scanResponses());
        }

        return value;
    }

    private static AiGatewayProjectSettings toSettings(Long projectId, Map<String, ?> value) {
        return new AiGatewayProjectSettings(
            projectId,
            (Boolean) value.get(KEY_REDACT_PII),
            (Boolean) value.get(KEY_REDACT_SECRETS),
            (String) value.get(KEY_BLOCKED_TERMS),
            (Boolean) value.get(KEY_MODERATION_ENABLED),
            (Boolean) value.get(KEY_INJECTION_DETECTION_ENABLED),
            (Boolean) value.get(KEY_SCAN_RESPONSES));
    }
}
