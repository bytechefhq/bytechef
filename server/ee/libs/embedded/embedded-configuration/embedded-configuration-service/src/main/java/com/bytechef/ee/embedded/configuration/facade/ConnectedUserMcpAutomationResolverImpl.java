/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserMcpAutomationResolverImpl implements ConnectedUserMcpAutomationResolver {

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final WorkflowConnectionSlot workflowConnectionSlot;

    @SuppressFBWarnings("EI")
    public ConnectedUserMcpAutomationResolverImpl(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        WorkflowConnectionSlot workflowConnectionSlot) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.workflowConnectionSlot = workflowConnectionSlot;
    }

    @Override
    public Optional<CallableAutomationDTO> fetchCallableAutomation(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid) {

        AutomationProjectConfiguration configuration = automationProjectConfigurationService.getConfiguration(
            automationProjectConfigurationId);

        if (!configuration.isEnabled() ||
            !automationProjectConfigurationService.isWorkflowEnabled(configuration, UUID.fromString(workflowUuid))) {

            return Optional.empty();
        }

        Optional<ConnectedUserProjectWorkflow> referenceOptional = fetchReference(
            connectedUserId, configuration, workflowUuid);

        if (referenceOptional.isEmpty()) {
            return Optional.empty();
        }

        ConnectedUserProjectWorkflow reference = referenceOptional.get();

        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (reference.isDangling() || projectDeploymentId == null) {
            return Optional.empty();
        }

        Optional<ProjectDeployment> projectDeploymentOptional = connectedUserReferenceDeploymentManager
            .fetchDeployment(projectDeploymentId);

        if (projectDeploymentOptional.isEmpty()) {
            return Optional.empty();
        }

        ProjectDeployment projectDeployment = projectDeploymentOptional.get();

        if (!Objects.equals(projectDeployment.getProjectId(), configuration.getProjectId())) {
            return Optional.empty();
        }

        Optional<ProjectDeploymentWorkflow> rowOptional = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeploymentId, workflowUuid);

        if (rowOptional.isEmpty()) {
            return Optional.empty();
        }

        ProjectDeploymentWorkflow row = rowOptional.get();

        String missingConnectionComponentName = workflowConnectionSlot.findMissingRequiredComponentName(
            row.getWorkflowId(), row.getConnections());

        return Optional.of(
            new CallableAutomationDTO(
                reference.getId(), projectDeploymentId, row.getWorkflowId(), row.getInputs(),
                missingConnectionComponentName));
    }

    @Override
    public Optional<Long> fetchConnectedUserProjectWorkflowId(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid) {

        AutomationProjectConfiguration configuration = automationProjectConfigurationService.getConfiguration(
            automationProjectConfigurationId);

        return fetchReference(connectedUserId, configuration, workflowUuid)
            .map(ConnectedUserProjectWorkflow::getId);
    }

    private Optional<ConnectedUserProjectWorkflow> fetchReference(
        long connectedUserId, AutomationProjectConfiguration configuration, String workflowUuid) {

        Optional<ConnectedUserProjectConfiguration> assignmentOptional = connectedUserProjectConfigurationService
            .fetchAssignment(connectedUserId, configuration.getProjectId());

        if (assignmentOptional.isEmpty()) {
            return Optional.empty();
        }

        ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

        if (!Objects.equals(assignment.getConfigurationId(), configuration.getId())) {
            return Optional.empty();
        }

        for (ConnectedUserProject connectedUserProject : connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)) {

            for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflowService
                .getConnectedUserProjectWorkflows(connectedUserProject.getId())) {

                if (Objects.equals(connectedUserProjectWorkflow.getAutomationUuid(), workflowUuid)) {
                    return Optional.of(connectedUserProjectWorkflow);
                }
            }
        }

        return Optional.empty();
    }
}
