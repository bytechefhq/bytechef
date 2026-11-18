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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the static webhook url of a connected user's workflow that starts with a static webhook trigger (e.g.
 * Webhook): an automation workflow, the same url the automation project deployment exposes, or an integration instance
 * workflow, the url its trigger was enabled with.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ConnectedUserWorkflowWebhookUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserWorkflowWebhookUrlResolver.class);

    private static final Set<String> EMBEDDED_API_TRIGGER_COMPONENT_NAMES = Set.of("appEvent", "request");

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
     *                            automation project's for a reference
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

        return resolve(
            PlatformType.AUTOMATION, projectDeploymentId, workflowUuid, workflowService.getWorkflow(workflowId.get()));
    }

    @Nullable
    String resolveIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, @Nullable Workflow workflow) {

        if (workflow == null || webhookUrl == null) {
            return null;
        }

        return resolve(PlatformType.EMBEDDED, integrationInstanceId, workflowUuid, workflow);
    }

    boolean hasStaticWebhookTrigger(@Nullable Workflow workflow) {
        if (workflow == null) {
            return false;
        }

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
            if (isStaticWebhookTrigger(WorkflowNodeType.ofType(workflowTrigger.getType()))) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private String resolve(PlatformType platformType, long id, String workflowUuid, Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (isStaticWebhookTrigger(workflowNodeType)) {
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    platformType, id, workflowUuid, workflowTrigger.getName());

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

        if (EMBEDDED_API_TRIGGER_COMPONENT_NAMES.contains(workflowNodeType.name())) {
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
