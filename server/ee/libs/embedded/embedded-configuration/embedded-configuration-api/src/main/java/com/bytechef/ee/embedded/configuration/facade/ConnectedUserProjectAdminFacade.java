/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectAdminFacade {

    void deleteProjectWorkflow(long connectedUserProjectWorkflowId);

    void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable);

    /**
     * Returns the connected user's configuration assignments.
     *
     * @param connectedUserId the connected user id
     * @return the configuration the connected user is assigned to in each automation project, in project id order
     */
    List<ConnectedUserAutomationProjectConfigurationDTO> getConnectedUserProjectConfigurations(long connectedUserId);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment);

    /**
     * Returns the connections and inputs a provisioned workflow currently runs with, leaving out the ones its
     * configuration provides.
     *
     * @param connectedUserProjectWorkflowId the id of the connected user's provisioned workflow
     * @return the current connections and inputs
     */
    ConnectedUserWorkflowReferenceValuesDTO getProjectWorkflowReferenceValues(long connectedUserProjectWorkflowId);

    List<ProvisionableAutomationProjectDTO> getProvisionableProjects(long connectedUserId);

    /**
     * Assigns the connected user to the configuration, whatever its permission expression says, and provisions the
     * workflows the configuration releases.
     *
     * @param connectedUserId the connected user id
     * @param configurationId the configuration id; must be the configuration the connected user is already assigned to
     *                        in the project, if any, since a change of configuration goes through a move
     * @param workflows       the workflows to provision
     * @throws com.bytechef.exception.ConfigurationException with {@code CONFIGURATION_PROJECT_MISMATCH} when the
     *                                                       configuration is disabled, belongs to another environment
     *                                                       or to a project not released to the connected user, or the
     *                                                       connected user is assigned to another configuration of the
     *                                                       project, and with {@code WORKFLOW_NOT_PROVISIONABLE} when
     *                                                       the workflows are empty or the configuration does not
     *                                                       release one of them
     */
    void provisionProjectWorkflows(
        long connectedUserId, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows);
}
