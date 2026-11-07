/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.service;

import com.bytechef.ee.platform.component.rule.ComponentRuleSettings;
import com.bytechef.ee.platform.component.rule.ComponentRuleSettingsService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class ComponentRuleSettingsServiceImpl implements ComponentRuleSettingsService {

    private static final String KEY_APPROVAL_EXPIRES_IN_HOURS = "approvalExpiresInHours";
    private static final String KEY_OBSERVE_MODE = "observeMode";

    private static final Logger log = LoggerFactory.getLogger(ComponentRuleSettingsServiceImpl.class);

    private final PropertyService propertyService;

    public ComponentRuleSettingsServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public ComponentRuleSettings getSettings(@Nullable Long workspaceId) {
        try {
            if (workspaceId != null) {
                ComponentRuleSettings workspaceComponentRuleSettings = fetchSettings(Scope.WORKSPACE, workspaceId);

                if (workspaceComponentRuleSettings != null) {
                    return workspaceComponentRuleSettings;
                }
            }

            ComponentRuleSettings tenantComponentRuleSettings = fetchSettings(Scope.PLATFORM, null);

            return tenantComponentRuleSettings == null ? ComponentRuleSettings.DEFAULT : tenantComponentRuleSettings;
        } catch (RuntimeException exception) {
            log.warn(
                "Could not read component rule settings; falling back to the default (observe mode off)",
                exception);

            return ComponentRuleSettings.DEFAULT;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComponentRuleSettings> fetchWorkspaceOverride(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(fetchSettings(Scope.WORKSPACE, workspaceId));
        } catch (RuntimeException exception) {
            log.warn("Could not read the workspace's component rule settings override", exception);

            return Optional.empty();
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentRuleSettings saveSettings(ComponentRuleSettings componentRuleSettings, @Nullable Long workspaceId) {
        propertyService.save(
            ComponentRuleSettings.PROPERTY_KEY, toMap(componentRuleSettings), scopeOf(workspaceId), workspaceId);

        return componentRuleSettings;
    }

    private @Nullable ComponentRuleSettings fetchSettings(Scope scope, @Nullable Long scopeId) {
        return propertyService.fetchProperty(ComponentRuleSettings.PROPERTY_KEY, scope, scopeId)
            .map(property -> toSettings(property.getValue()))
            .orElse(null);
    }

    private static Scope scopeOf(@Nullable Long workspaceId) {
        return workspaceId == null ? Scope.PLATFORM : Scope.WORKSPACE;
    }

    private static Map<String, Object> toMap(ComponentRuleSettings componentRuleSettings) {
        return Map.of(
            KEY_OBSERVE_MODE, componentRuleSettings.observeMode(),
            KEY_APPROVAL_EXPIRES_IN_HOURS, componentRuleSettings.approvalExpiresInHours());
    }

    private static ComponentRuleSettings toSettings(Map<String, ?> value) {
        Object observeMode = value.get(KEY_OBSERVE_MODE);
        Object approvalExpiresInHours = value.get(KEY_APPROVAL_EXPIRES_IN_HOURS);

        return new ComponentRuleSettings(
            observeMode instanceof Boolean observeModeBoolean
                ? observeModeBoolean : ComponentRuleSettings.DEFAULT.observeMode(),
            approvalExpiresInHours instanceof Number approvalExpiresInHoursNumber
                ? approvalExpiresInHoursNumber.intValue() : ComponentRuleSettings.DEFAULT.approvalExpiresInHours());
    }
}
