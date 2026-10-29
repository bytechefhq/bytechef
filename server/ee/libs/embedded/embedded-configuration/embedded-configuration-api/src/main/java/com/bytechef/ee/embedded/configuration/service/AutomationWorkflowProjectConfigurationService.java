/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectConfigurationService {

    AutomationWorkflowProjectConfiguration create(
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible);

    void delete(long id);

    Optional<AutomationWorkflowProjectConfiguration> fetchConfiguration(long projectId, long environmentId);

    AutomationWorkflowProjectConfiguration getConfiguration(long id);

    List<AutomationWorkflowProjectConfiguration> getConfigurations(long projectId);

    List<AutomationWorkflowProjectConfiguration> getConfigurations(Set<Long> projectIds);

    /**
     * Takes a transaction-scoped lock that serializes configuration rollouts for one project and environment; it is
     * released when the caller's transaction ends. It never blocks writes to the configuration itself. Must be called
     * inside a transaction.
     */
    void lockRollout(long projectId, long environmentId);

    boolean isWorkflowEnabled(AutomationWorkflowProjectConfiguration configuration, UUID workflowUuid);

    AutomationWorkflowProjectConfiguration update(
        long id, int projectVersion, boolean enabled, boolean automationHubVisible);

    AutomationWorkflowProjectConfiguration updateEnabled(long id, boolean enabled);

    AutomationWorkflowProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled);
}
