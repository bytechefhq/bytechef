/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserProjectWorkflowDTO(
    long id, long connectedUserId, boolean enabled, Instant lastExecutionDate, long projectId, WorkflowDTO workflow,
    String workflowUuid, Integer workflowVersion, Kind kind, String automationUuid,
    String copiedFromWorkflowUuid, boolean dangling, List<ConnectedUserWorkflowTemplateDTO.Component> components,
    List<ConnectedUserWorkflowTemplateDTO.Input> inputs, Map<String, ?> inputValues, Map<String, Long> connections,
    @Nullable String attentionReason, @Nullable String staticWebhookUrl) {
    public enum Kind {
        COPY, REFERENCE
    }

    public ConnectedUserProjectWorkflowDTO(
        long connectedUserId, ConnectedUserProjectWorkflow connectedUserProjectWorkflow, boolean enabled,
        Instant lastExecutionDate, ProjectWorkflow projectWorkflow, WorkflowDTO workflow,
        List<ConnectedUserWorkflowTemplateDTO.Component> components) {
        this(
            connectedUserId, connectedUserProjectWorkflow, enabled, lastExecutionDate, projectWorkflow, workflow,
            components, List.of(), Map.of());
    }

    public ConnectedUserProjectWorkflowDTO(
        long connectedUserId, ConnectedUserProjectWorkflow connectedUserProjectWorkflow, boolean enabled,
        Instant lastExecutionDate, ProjectWorkflow projectWorkflow, WorkflowDTO workflow,
        List<ConnectedUserWorkflowTemplateDTO.Component> components,
        List<ConnectedUserWorkflowTemplateDTO.Input> inputs, Map<String, ?> inputValues) {
        this(
            connectedUserProjectWorkflow.getId(), connectedUserId, enabled, lastExecutionDate,
            projectWorkflow.getProjectId(), workflow, projectWorkflow.getUuidAsString(),
            connectedUserProjectWorkflow.getWorkflowVersion(), Kind.COPY, null,
            connectedUserProjectWorkflow.getCopiedFromWorkflowUuid(), false, components, inputs, inputValues, Map.of(),
            null, null);
    }

    public static ConnectedUserProjectWorkflowDTO ofReference(
        long connectedUserId, ConnectedUserProjectWorkflow reference, WorkflowDTO automation,
        List<ConnectedUserWorkflowTemplateDTO.Component> components) {
        return ofReference(connectedUserId, reference, automation, components, List.of(), Map.of(), Map.of(), null);
    }

    public static ConnectedUserProjectWorkflowDTO ofReference(
        long connectedUserId, ConnectedUserProjectWorkflow reference, WorkflowDTO automation,
        List<ConnectedUserWorkflowTemplateDTO.Component> components,
        List<ConnectedUserWorkflowTemplateDTO.Input> inputs, Map<String, ?> inputValues,
        Map<String, Long> connections, @Nullable String attentionReason) {
        return new ConnectedUserProjectWorkflowDTO(
            reference.getId(), connectedUserId, reference.isEnabled(), null, 0L, automation,
            reference.getAutomationUuid(), reference.getWorkflowVersion(), Kind.REFERENCE,
            reference.getAutomationUuid(), null, reference.isDangling(), components, inputs, inputValues, connections,
            attentionReason, null);
    }

    public ConnectedUserProjectWorkflowDTO withAttentionReason(@Nullable String attentionReason) {
        return new ConnectedUserProjectWorkflowDTO(
            id, connectedUserId, enabled, lastExecutionDate, projectId, workflow, workflowUuid, workflowVersion, kind,
            automationUuid, copiedFromWorkflowUuid, dangling, components, inputs, inputValues, connections,
            attentionReason, staticWebhookUrl);
    }

    /**
     * @param staticWebhookUrl the url that starts the workflow through its static webhook trigger, {@code null} if it
     *                         has none
     * @return a copy carrying the static webhook url
     */
    public ConnectedUserProjectWorkflowDTO withStaticWebhookUrl(@Nullable String staticWebhookUrl) {
        return new ConnectedUserProjectWorkflowDTO(
            id, connectedUserId, enabled, lastExecutionDate, projectId, workflow, workflowUuid, workflowVersion, kind,
            automationUuid, copiedFromWorkflowUuid, dangling, components, inputs, inputValues, connections,
            attentionReason, staticWebhookUrl);
    }
}
