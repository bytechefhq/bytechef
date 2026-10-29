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
     * Returns the connection slots (with the connections an administrator can provide), the inputs and the values
     * already provided for one workflow of the configuration, at the configuration's pinned project version.
     */
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid);

    AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(long configurationId, int projectVersion);

    void updateConfiguration(long id, int projectVersion, boolean enabled, boolean automationHubVisible);

    /**
     * Sets the connections (component name to connection id) and input values (input name to value) the configuration
     * provides to every connected user of the workflow. Empty maps clear them.
     */
    void updateConfigurationWorkflow(
        long id, String workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs);
}
