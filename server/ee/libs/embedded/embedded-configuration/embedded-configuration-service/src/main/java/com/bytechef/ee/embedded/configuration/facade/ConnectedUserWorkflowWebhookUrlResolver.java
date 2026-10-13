/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the static webhook url of a connected user's automation workflow, the same url the automation project
 * deployment exposes for a workflow that starts with a static webhook trigger (e.g. Request or Webhook).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ConnectedUserWorkflowWebhookUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserWorkflowWebhookUrlResolver.class);

    private final ProjectWorkflowService projectWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    ConnectedUserWorkflowWebhookUrlResolver(
        ApplicationProperties applicationProperties, ProjectWorkflowService projectWorkflowService,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService) {

        this.projectWorkflowService = projectWorkflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.workflowService = workflowService;
    }

    /**
     * @param projectDeploymentId the deployment the workflow runs on, the connected user's own for a copy, the
     *                            automation workflow project's for a reference
     * @param workflowUuid        the workflow uuid
     * @return the static webhook url, or {@code null} if the workflow is not deployed or does not start with a static
     *         webhook trigger
     */
    @Nullable
    String resolve(@Nullable Long projectDeploymentId, @Nullable String workflowUuid) {
        if (projectDeploymentId == null || workflowUuid == null || webhookUrl == null) {
            return null;
        }

        Optional<String> workflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(
            projectDeploymentId, workflowUuid);

        if (workflowId.isEmpty()) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(workflowId.get());

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (isStaticWebhookTrigger(workflowNodeType)) {
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, projectDeploymentId, workflowUuid, workflowTrigger.getName());

                return webhookUrl.replace("{id}", workflowExecutionId.toString());
            }
        }

        return null;
    }

    private boolean isStaticWebhookTrigger(WorkflowNodeType workflowNodeType) {
        String operation = workflowNodeType.operation();

        if (operation == null || Objects.equals(operation, "manual")) {
            return false;
        }

        TriggerDefinition triggerDefinition;

        try {
            triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.name(), workflowNodeType.version(), operation);
        } catch (RuntimeException exception) {
            log.error("Failed to get trigger definition for workflow trigger type={}", workflowNodeType.name(),
                exception);

            return false;
        }

        return triggerDefinition != null && triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK;
    }
}
