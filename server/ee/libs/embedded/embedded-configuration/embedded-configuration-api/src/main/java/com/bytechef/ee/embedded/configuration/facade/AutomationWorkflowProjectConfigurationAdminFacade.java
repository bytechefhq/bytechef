/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectConfigurationAdminFacade {

    long createConfiguration(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible);

    /**
     * Deletes the configuration together with the reference workflows and reference deployments of every connected user
     * assigned to it, and those users' assignments. Workflow copies are never touched.
     *
     * @param id the configuration id
     */
    void deleteConfiguration(long id);

    void enableConfiguration(long id, boolean enable);

    void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable);

    AutomationWorkflowProjectConfigurationDTO getConfiguration(long id);

    /**
     * Returns the connected users assigned to the configuration, in connected user id order.
     *
     * @param id the configuration id
     * @return the assigned connected users
     */
    List<AutomationWorkflowProjectConfigurationConnectedUserDTO> getConfigurationConnectedUsers(long id);

    List<AutomationWorkflowProjectConfigurationDTO> getConfigurations(long projectId);

    /**
     * Returns the connections and inputs an administrator can provide for a workflow, with the values provided.
     *
     * @param configurationId the configuration id
     * @param workflowUuid    the workflow uuid
     * @return the providable and provided values at the configuration's pinned project version
     */
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid);

    /**
     * Loads the options of a component-referencing input of a workflow, using one of the administrator's connections.
     *
     * @param configurationId       the configuration id
     * @param workflowUuid          the workflow uuid
     * @param inputName             the workflow input name
     * @param propertyName          the property of the referenced component input group
     * @param connectionId          the administrator's connection of the referenced component
     * @param lookupDependsOnValues the values of the group's other properties the options depend on
     * @param searchText            the optional search text
     * @return the options
     */
    List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        getConfigurationWorkflowInputOptions(
            long configurationId, String workflowUuid, String inputName, String propertyName, long connectionId,
            Map<String, ?> lookupDependsOnValues, @Nullable String searchText);

    /**
     * Returns what {@link #deleteConfiguration(long)} would remove.
     *
     * @param id the configuration id
     * @return the number of assigned connected users and of their reference workflows
     */
    AutomationWorkflowProjectConfigurationDeleteImpactDTO getDeleteImpact(long id);

    /**
     * Returns what moving the connected users from the source to the target changes for them: the version impact of the
     * target's project version, counting only those of the given users who are assigned to the source.
     *
     * @param sourceId         the configuration the users are assigned to
     * @param targetId         the configuration they would move to
     * @param connectedUserIds the connected user ids
     * @return the impact of the target's project version on those users
     */
    AutomationWorkflowProjectConfigurationVersionImpactDTO getMoveImpact(
        long sourceId, long targetId, List<Long> connectedUserIds);

    AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(long configurationId, int projectVersion);

    /**
     * Moves the connected users from the source configuration to the target, one transaction per user, so that one
     * user's failure leaves the others moved. Each moved user's reference deployment rolls to the target's project
     * version and takes the target's enabled state and provided values. Workflow copies are never touched.
     *
     * @param sourceId         the configuration the users are assigned to
     * @param targetId         the configuration of the same project and environment to move them to
     * @param connectedUserIds the connected user ids
     * @return the moved, skipped and needs-attention connected users
     */
    ConfigurationMoveResultDTO moveConnectedUsers(long sourceId, long targetId, List<Long> connectedUserIds);

    void updateConfiguration(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible);

    /**
     * Sets the connections and input values the configuration provides to every connected user of the workflow.
     *
     * @param id                  the configuration id
     * @param workflowUuid        the workflow uuid
     * @param providedConnections the connection ids keyed by component name; empty clears them
     * @param providedInputs      the input values keyed by input name; empty clears them
     */
    void updateConfigurationWorkflow(
        long id, String workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs);
}
