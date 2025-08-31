/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectFacade {

    String createProjectWorkflow(
        String externalUserId, String definition, Environment environment);

    void deleteProjectWorkflow(String externalUserId, String workflowReferenceCode, Environment environment);

    void enableProjectWorkflow(
        String externalUserId, String workflowReferenceCode, boolean enable, Long environmentId);

    ConnectedUserProjectWorkflowDTO getConnectedUserProjectWorkflow(
        String externalUserId, String workflowReferenceCode, Long environmentId);

    List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment);

    void publishProjectWorkflow(
        String externalUserId, String workflowReferenceCode, String description, Long environmentId);

    void updateProjectWorkflow(
        String externalUserId, String workflowReferenceCode, String definition, Environment environment);

    void updateWorkflowConfigurationConnection(
        String externalUserId, String workflowReferenceCode, String workflowNodeName, String workflowConnectionKey,
        long connectionId, Environment environment);
}
