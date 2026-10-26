/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.trigger;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.TriggerJobParameterContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreWorkflowContextJobParameterContributor implements TriggerJobParameterContributor {

    public static final String WORKSPACE_ID_KEY = "contextStore.workspaceId";

    private static final Logger log =
        LoggerFactory.getLogger(ContextStoreWorkflowContextJobParameterContributor.class);

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI2")
    public ContextStoreWorkflowContextJobParameterContributor(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public Map<String, ?> contribute(Map<String, ?> workflowMetadataMap, WorkflowExecutionId workflowExecutionId) {
        if (workflowExecutionId.getType() != PlatformType.AUTOMATION) {
            return Map.of();
        }

        long projectDeploymentId = workflowExecutionId.getJobPrincipalId();

        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);
            Project project = projectService.getProject(projectDeployment.getProjectId());

            Long workspaceId = project.getWorkspaceId();

            if (workspaceId == null) {
                return Map.of();
            }

            return Map.of(WORKSPACE_ID_KEY, workspaceId);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve workspaceId for ProjectDeployment {} during trigger contribution: {}",
                projectDeploymentId, exception.getMessage());

            return Map.of();
        }
    }
}
