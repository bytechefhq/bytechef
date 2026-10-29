/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationWorkflowProjectConfigurationServiceClient
    implements AutomationWorkflowProjectConfigurationService {

    @Override
    public AutomationWorkflowProjectConfiguration create(
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AutomationWorkflowProjectConfiguration> fetchConfiguration(long projectId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AutomationWorkflowProjectConfiguration> fetchConfigurationForUpdate(
        long projectId, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationWorkflowProjectConfiguration getConfiguration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectConfiguration> getConfigurations(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectConfiguration> getConfigurations(Set<Long> projectIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWorkflowEnabled(AutomationWorkflowProjectConfiguration configuration, UUID workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationWorkflowProjectConfiguration update(
        long id, int projectVersion, boolean enabled, boolean automationHubVisible) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationWorkflowProjectConfiguration updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationWorkflowProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
