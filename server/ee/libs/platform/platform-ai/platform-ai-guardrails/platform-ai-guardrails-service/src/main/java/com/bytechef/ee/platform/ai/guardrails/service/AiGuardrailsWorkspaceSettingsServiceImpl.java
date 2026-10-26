/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class AiGuardrailsWorkspaceSettingsServiceImpl implements AiGuardrailsWorkspaceSettingsService {

    private static final String KEY_BLOCKED_TERMS = "blockedTerms";
    private static final String KEY_BLOCKING_MODE = "blockingMode";
    private static final String KEY_INJECTION_DETECTION_ENABLED = "injectionDetectionEnabled";
    private static final String KEY_MIN_CONFIDENCE = "minConfidence";
    private static final String KEY_MODERATION_ENABLED = "moderationEnabled";
    private static final String KEY_REDACT_MCP_RESULTS = "redactMcpResults";
    private static final String KEY_REDACT_PII = "redactPii";
    private static final String KEY_REDACT_SECRETS = "redactSecrets";
    private static final String KEY_RESTORE_INTO_WORKFLOW_OUTPUT = "restoreIntoWorkflowOutput";
    private static final String KEY_SCAN_RESPONSES = "scanResponses";

    private final PropertyService propertyService;

    AiGuardrailsWorkspaceSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGuardrailsWorkspaceSettings> fetchSettings(@Nullable Long workspaceId) {
        AiGuardrailsSettingsScope scope =
            workspaceId == null ? AiGuardrailsSettingsScope.PLATFORM : AiGuardrailsSettingsScope.WORKSPACE;

        return propertyService
            .fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, scopeOf(scope), workspaceId)
            .map(property -> toSettings(scope, workspaceId, property.getValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGuardrailsWorkspaceSettings> fetchEmbeddedSettings() {
        return propertyService
            .fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Property.Scope.EMBEDDED, null)
            .map(property -> toSettings(AiGuardrailsSettingsScope.EMBEDDED, null, property.getValue()));
    }

    @Override
    public AiGuardrailsWorkspaceSettings saveSettings(AiGuardrailsWorkspaceSettings settings) {
        Validate.notNull(settings, "settings must not be null");

        Map<String, Object> value = toMap(settings);

        propertyService.save(
            AiGuardrailsWorkspaceSettings.PROPERTY_KEY, value, scopeOf(settings.scope()), settings.workspaceId());

        return settings;
    }

    private static Property.Scope scopeOf(AiGuardrailsSettingsScope scope) {
        return switch (scope) {
            case PLATFORM -> Property.Scope.PLATFORM;
            case WORKSPACE -> Property.Scope.WORKSPACE;
            case EMBEDDED -> Property.Scope.EMBEDDED;
        };
    }

    private static Map<String, Object> toMap(AiGuardrailsWorkspaceSettings settings) {
        Map<String, Object> value = new HashMap<>();

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

        if (settings.blockingMode() != null) {
            value.put(KEY_BLOCKING_MODE, settings.blockingMode()
                .name());
        }

        if (settings.minConfidence() != null) {
            value.put(KEY_MIN_CONFIDENCE, settings.minConfidence());
        }

        if (settings.redactMcpResults() != null) {
            value.put(KEY_REDACT_MCP_RESULTS, settings.redactMcpResults());
        }

        if (settings.restoreIntoWorkflowOutput() != null) {
            value.put(KEY_RESTORE_INTO_WORKFLOW_OUTPUT, settings.restoreIntoWorkflowOutput());
        }

        return value;
    }

    private static AiGuardrailsWorkspaceSettings toSettings(
        AiGuardrailsSettingsScope scope, @Nullable Long workspaceId, Map<String, ?> value) {

        return new AiGuardrailsWorkspaceSettings(
            scope,
            workspaceId,
            (Boolean) value.get(KEY_REDACT_PII),
            (Boolean) value.get(KEY_REDACT_SECRETS),
            (String) value.get(KEY_BLOCKED_TERMS),
            (Boolean) value.get(KEY_MODERATION_ENABLED),
            (Boolean) value.get(KEY_INJECTION_DETECTION_ENABLED),
            (Boolean) value.get(KEY_SCAN_RESPONSES),
            blockingModeValue(value),
            minConfidenceValue(value),
            (Boolean) value.get(KEY_REDACT_MCP_RESULTS),
            (Boolean) value.get(KEY_RESTORE_INTO_WORKFLOW_OUTPUT));
    }

    private static @Nullable Double minConfidenceValue(Map<String, ?> value) {
        Object raw = value.get(KEY_MIN_CONFIDENCE);

        return raw == null ? null : ((Number) raw).doubleValue();
    }

    private static BlockingMode blockingModeValue(Map<String, ?> value) {
        Object raw = value.get(KEY_BLOCKING_MODE);

        return raw == null ? BlockingMode.BLOCK : BlockingMode.valueOf((String) raw);
    }
}
