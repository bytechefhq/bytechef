/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationProjectConfigurationServiceClient
    implements AutomationProjectConfigurationService {

    @Override
    public AutomationProjectConfiguration create(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectConfiguration getConfiguration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectConfiguration> getConfigurations(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectConfiguration> getConfigurations(long projectId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectConfiguration> getConfigurations(Set<Long> projectIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockRollout(long projectId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWorkflowEnabled(AutomationProjectConfiguration configuration, UUID workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectConfiguration update(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectConfiguration updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectConfiguration updateWorkflowProvidedValues(
        long id, UUID workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs) {

        throw new UnsupportedOperationException();
    }
}
