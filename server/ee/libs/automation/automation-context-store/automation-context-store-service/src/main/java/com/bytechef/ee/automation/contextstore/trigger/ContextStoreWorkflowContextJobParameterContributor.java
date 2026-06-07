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
 * Phase 5 hook: injects the running workflow's {@code workspaceId} into every trigger-spawned job's
 * {@code __jobParameters} metadata under {@code contextStore.workspaceId}. The {@code contextStore.searchByStore}
 * action reads that value alongside {@code ActionContextAware#getEnvironmentId()} (already wired) to resolve a Context
 * Store by stable name at perform time, letting a single workflow definition promote from DEVELOPMENT → STAGING →
 * PRODUCTION without hardcoded ids.
 *
 * <p>
 * Why two separate sources for the two values: {@code environmentId} is already a first-class field on
 * {@link com.bytechef.platform.component.definition.ActionContextAware}, so the action reads it directly. {@code
 * workspaceId} has no equivalent slot — adding one would touch every {@code createActionContext} caller, but the
 * JobParameter contributor pattern carries it through without a single call-site change. Net result: the action gets
 * both values, the contract surface stays narrow.
 *
 * <p>
 * Scope: contributes ONLY when the firing workflow's principal is an automation-side {@link ProjectDeployment} (i.e.
 * {@link PlatformType#AUTOMATION}). Embedded principals, editor environment runs, and any principal that fails the
 * deployment lookup collapse to an empty contribution — the action's name-based path then errors with a clear message
 * rather than silently picking the wrong store. Co-exists with {@link ContextStoreTriggerJobParameterContributor}; keys
 * are namespaced under {@code contextStore.} so the merge never collides.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreWorkflowContextJobParameterContributor implements TriggerJobParameterContributor {

    /**
     * JobParameter key carrying the resolved workspaceId. Matches the namespace {@code contextStore.<value-key>}
     * pattern used by {@link ContextStoreTriggerJobParameterContributor} for {@code datastream.since}.
     */
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
        // Embedded principals don't map to a workspaceId via the (ProjectDeployment, Project) chain. Returning an
        // empty map keeps the contributor harmless for embedded workflows; the action's name path will error out
        // explicitly when invoked there rather than picking the wrong workspace.
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
            // Per the SPI contract: never throw. A transient lookup miss, a deleted deployment, or a missing project
            // collapses to an empty contribution rather than breaking trigger dispatch for the entire workflow.
            log.warn(
                "Failed to resolve workspaceId for ProjectDeployment {} during trigger contribution: {}",
                projectDeploymentId, exception.getMessage());

            return Map.of();
        }
    }
}
