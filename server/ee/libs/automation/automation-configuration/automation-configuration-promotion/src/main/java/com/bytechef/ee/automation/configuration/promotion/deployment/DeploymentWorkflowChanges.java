/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DeploymentWorkflowChanges {

    public static final String ASSET_TYPE = "DEPLOYMENT_WORKFLOW";

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DeploymentWorkflowChanges(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public List<PromotionChange> predict(
        @Nullable ProjectDeployment target, List<ProjectDeploymentWorkflow> sourceWorkflows,
        Map<String, ProjectWorkflow> sourceProjectWorkflowsByWorkflowId, String projectName) {

        Map<String, ProjectDeploymentWorkflow> targetWorkflowsByUuid =
            target == null ? Map.of() : targetWorkflowsByUuid(target);

        List<PromotionChange> changes = new ArrayList<>();
        Set<String> syncedUuids = new LinkedHashSet<>();

        for (ProjectDeploymentWorkflow sourceWorkflow : sourceWorkflows) {
            ProjectWorkflow sourceProjectWorkflow = sourceProjectWorkflowsByWorkflowId.get(
                sourceWorkflow.getWorkflowId());

            if (sourceProjectWorkflow == null) {
                continue;
            }

            String workflowUuid = sourceProjectWorkflow.getUuidAsString();

            if (!syncedUuids.add(workflowUuid)) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(sourceProjectWorkflow.getWorkflowId());
            ProjectDeploymentWorkflow targetWorkflow = targetWorkflowsByUuid.get(workflowUuid);

            if (targetWorkflow == null) {
                changes.add(change(Kind.CREATE, workflowUuid, projectName, workflow));
            } else if (!sameWorkflow(workflow, targetWorkflow.getWorkflowId())) {
                changes.add(change(Kind.UPDATE, workflowUuid, projectName, workflow));
            }
        }

        for (Map.Entry<String, ProjectDeploymentWorkflow> entry : targetWorkflowsByUuid.entrySet()) {
            String workflowUuid = entry.getKey();

            if (syncedUuids.contains(workflowUuid)) {
                continue;
            }

            ProjectDeploymentWorkflow targetWorkflow = entry.getValue();

            Workflow workflow = workflowService.getWorkflow(targetWorkflow.getWorkflowId());

            changes.add(change(Kind.DELETE, workflowUuid, projectName, workflow));
        }

        return changes;
    }

    private Map<String, ProjectDeploymentWorkflow> targetWorkflowsByUuid(ProjectDeployment target) {
        Map<String, String> uuidsByWorkflowId = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(
            Objects.requireNonNull(target.getProjectId(), "projectId"))) {

            uuidsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        Map<String, ProjectDeploymentWorkflow> targetWorkflowsByUuid = new LinkedHashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(Objects.requireNonNull(target.getId(), "id"))) {

            String workflowUuid = uuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflowUuid == null) {
                throw new ConfigurationException(
                    "Project workflow with workflowId=%s not found".formatted(
                        projectDeploymentWorkflow.getWorkflowId()),
                    WorkflowErrorType.WORKFLOW_NOT_FOUND);
            }

            targetWorkflowsByUuid.putIfAbsent(workflowUuid, projectDeploymentWorkflow);
        }

        return targetWorkflowsByUuid;
    }

    private boolean sameWorkflow(Workflow sourceWorkflow, String targetWorkflowId) {
        if (Objects.equals(sourceWorkflow.getId(), targetWorkflowId)) {
            return true;
        }

        Workflow targetWorkflow = workflowService.getWorkflow(targetWorkflowId);

        return WorkflowDefinitions.sameDefinition(sourceWorkflow, targetWorkflow);
    }

    private static PromotionChange change(Kind kind, String workflowUuid, String projectName, Workflow workflow) {
        String label = workflow.getLabel();

        return new PromotionChange(
            kind, ASSET_TYPE, workflowUuid, projectName, label == null ? workflowUuid : label, kind == Kind.DELETE);
    }
}
