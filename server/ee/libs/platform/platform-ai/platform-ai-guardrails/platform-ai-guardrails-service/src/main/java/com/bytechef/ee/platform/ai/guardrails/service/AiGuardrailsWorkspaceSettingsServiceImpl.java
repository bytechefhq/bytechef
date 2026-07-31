/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

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
 * Backs {@link AiGuardrailsWorkspaceSettings} with a single {@link Property} row per workspace, keyed by
 * {@link AiGuardrailsWorkspaceSettings#PROPERTY_KEY}. Reuses the platform configuration store rather than introducing a
 * dedicated table, mirroring {@code AiGatewayWorkspaceSettingsServiceImpl}'s storage shape and map&lt;-&gt;record
 * conversion style.
 *
 * <p>
 * <strong>Scope decision for the tenant default row</strong> (a {@code null} {@code workspaceId}): stored under
 * {@link Property.Scope#PLATFORM} with a {@code null} {@code scopeId} -- NOT {@code Scope.WORKSPACE} with a sentinel id
 * such as {@code 0L}. Rationale, from reading {@link Property.Scope} and {@link PropertyService}:
 * <ul>
 * <li>{@code PropertyService}'s {@code scopeId} parameter is already {@code @Nullable Long}, and
 * {@code PropertyServiceImpl} branches on a {@code null} scopeId as a first-class case (it looks the row up by key +
 * scope alone, e.g. {@code findByKeyAndScope}) -- {@code null} is a real, supported value, not a workaround.</li>
 * <li>{@code Scope.WORKSPACE} with {@code scopeId = 0L} would alias a real workspace whose id happens to be {@code 0},
 * or collide with whatever sentinel a future id scheme picks; a scope-less row cannot be confused with a real
 * workspace's row.</li>
 * <li>This is the established convention elsewhere in the platform for a scope-less/tenant-wide row --
 * {@code AiProviderConnectionSourceImpl} and the {@code "mcp.server"} property both use {@code Scope.PLATFORM} with a
 * {@code null} scopeId for the same "no natural scope id" shape. {@code AiGatewayWorkspaceSettingsServiceImpl} itself
 * has no null-workspace handling to copy (it requires a non-null workspaceId), so this is a new decision for this
 * service, not a copy of an existing one.</li>
 * </ul>
 *
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
    private static final String KEY_MODERATION_ENABLED = "moderationEnabled";
    private static final String KEY_REDACT_PII = "redactPii";
    private static final String KEY_REDACT_SECRETS = "redactSecrets";
    private static final String KEY_SCAN_RESPONSES = "scanResponses";

    private final PropertyService propertyService;

    AiGuardrailsWorkspaceSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGuardrailsWorkspaceSettings> fetchSettings(@Nullable Long workspaceId) {
        return propertyService
            .fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, scopeOf(workspaceId), workspaceId)
            .map(property -> toSettings(workspaceId, property.getValue()));
    }

    @Override
    public AiGuardrailsWorkspaceSettings saveSettings(AiGuardrailsWorkspaceSettings settings) {
        Validate.notNull(settings, "settings must not be null");

        Map<String, Object> value = toMap(settings);

        propertyService.save(
            AiGuardrailsWorkspaceSettings.PROPERTY_KEY, value, scopeOf(settings.workspaceId()),
            settings.workspaceId());

        return settings;
    }

    private static Property.Scope scopeOf(@Nullable Long workspaceId) {
        return workspaceId == null ? Property.Scope.PLATFORM : Property.Scope.WORKSPACE;
    }

    private static Map<String, Object> toMap(AiGuardrailsWorkspaceSettings settings) {
        Map<String, Object> value = new HashMap<>();

        // Only persist non-null overrides -- null means "not set at this level" (it unions with the GLOBAL
        // properties in AiGuardrails#resolvePolicy, not with the tenant-default row) and must not collide with an
        // explicit override of a different field.
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

        return value;
    }

    private static AiGuardrailsWorkspaceSettings toSettings(@Nullable Long workspaceId, Map<String, ?> value) {
        return new AiGuardrailsWorkspaceSettings(
            workspaceId,
            (Boolean) value.get(KEY_REDACT_PII),
            (Boolean) value.get(KEY_REDACT_SECRETS),
            (String) value.get(KEY_BLOCKED_TERMS),
            (Boolean) value.get(KEY_MODERATION_ENABLED),
            (Boolean) value.get(KEY_INJECTION_DETECTION_ENABLED),
            (Boolean) value.get(KEY_SCAN_RESPONSES),
            blockingModeValue(value));
    }

    private static BlockingMode blockingModeValue(Map<String, ?> value) {
        Object raw = value.get(KEY_BLOCKING_MODE);

        return raw == null ? BlockingMode.BLOCK : BlockingMode.valueOf((String) raw);
    }
}
