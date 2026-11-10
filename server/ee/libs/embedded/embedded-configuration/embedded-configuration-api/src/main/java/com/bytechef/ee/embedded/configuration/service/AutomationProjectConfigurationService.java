/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationProjectConfigurationService {

    AutomationProjectConfiguration create(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible);

    void delete(long id);

    AutomationProjectConfiguration getConfiguration(long id);

    List<AutomationProjectConfiguration> getConfigurations(long projectId);

    List<AutomationProjectConfiguration> getConfigurations(long projectId, long environmentId);

    List<AutomationProjectConfiguration> getConfigurations(Set<Long> projectIds);

    /**
     * Takes a transaction-scoped lock serializing rollouts of a project and environment; call inside a transaction.
     *
     * @param projectId     the project id
     * @param environmentId the environment id
     */
    void lockRollout(long projectId, long environmentId);

    boolean isWorkflowEnabled(AutomationProjectConfiguration configuration, UUID workflowUuid);

    AutomationProjectConfiguration update(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible);

    AutomationProjectConfiguration updateEnabled(long id, boolean enabled);

    AutomationProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled);

    /**
     * Replaces the connections and input values the configuration provides to every connected user of the workflow.
     *
     * @param id                  the configuration id
     * @param workflowUuid        the workflow uuid
     * @param providedConnections the connection ids keyed by component name; empty clears them
     * @param providedInputs      the input values keyed by input name; empty clears them
     * @return the updated configuration
     */
    AutomationProjectConfiguration updateWorkflowProvidedValues(
        long id, UUID workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs);
}
