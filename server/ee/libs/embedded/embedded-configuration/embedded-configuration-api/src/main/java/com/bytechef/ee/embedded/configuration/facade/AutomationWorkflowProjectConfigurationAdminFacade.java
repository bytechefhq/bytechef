/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
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
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible);

    void deleteConfiguration(long id);

    void enableConfiguration(long id, boolean enable);

    void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable);

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

    AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(long configurationId, int projectVersion);

    void updateConfiguration(long id, int projectVersion, boolean enabled, boolean automationHubVisible);

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
