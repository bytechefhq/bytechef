/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectFacade {

    String copyWorkflowTemplate(String externalUserId, String workflowUuid, Environment environment);

    String createProjectWorkflow(
        String externalUserId, String definition, Environment environment);

    String createProjectWorkflow(
        String externalUserId, String prompt, @Nullable String systemPrompt, Environment environment,
        boolean generate);

    void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment);

    void deleteProjectWorkflow(long connectedUserProjectWorkflowId);

    void enableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, Long environmentId);

    void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable);

    ConnectedUserProjectWorkflowDTO getConnectedUserProjectWorkflow(
        String externalUserId, String workflowUuid, Long environmentId);

    List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(String externalUserId, Environment environment);

    CopilotChatContextDTO prepareCopilotChat(String externalUserId, String workflowUuid, Environment environment);

    void publishProjectWorkflow(
        String externalUserId, String workflowUuid, String description, Long environmentId);

    void updateProjectWorkflow(
        String externalUserId, String workflowUuid, String definition, Environment environment);

    String updateProjectWorkflow(
        String externalUserId, String workflowUuid, String prompt, Environment environment, boolean generate);

    void updateWorkflowConfigurationConnection(
        String externalUserId, String workflowUuid, String workflowNodeName, String workflowConnectionKey,
        long connectionId, Environment environment);
}
