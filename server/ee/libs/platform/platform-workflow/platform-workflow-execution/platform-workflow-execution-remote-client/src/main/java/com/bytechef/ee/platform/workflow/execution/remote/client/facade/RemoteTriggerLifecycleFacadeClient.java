/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerLifecycleFacadeClient implements TriggerLifecycleFacade {

    private static final String TRIGGER_LIFECYCLE_FACADE = "/remote/trigger-lifecycle-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerLifecycleFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public boolean executeTriggerDisable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId) {

        Boolean disabled = loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable")
                .build(),
            new TriggerRequest(
                workflowId, workflowExecutionId.toString(), triggerWorkflowNodeType, triggerParameters, connectionId,
                null, -1),
            Boolean.class);

        return Boolean.TRUE.equals(disabled);
    }

    @Override
    public boolean executeTriggerEnableUndo(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, WebhookEnableOutput enableOutput,
        @Nullable Object previousTriggerState) {

        Boolean undone = loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable-undo")
                .build(),
            new TriggerEnableUndoRequest(
                workflowId, workflowExecutionId.toString(), triggerWorkflowNodeType, triggerParameters, connectionId,
                enableOutput, previousTriggerState),
            Boolean.class);

        return Boolean.TRUE.equals(undone);
    }

    @Override
    public @Nullable WebhookEnableOutput executeTriggerEnable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl, long environmentId) {

        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable")
                .build(),
            new TriggerRequest(
                workflowId, workflowExecutionId.toString(), triggerWorkflowNodeType, triggerParameters, connectionId,
                webhookUrl, environmentId),
            WebhookEnableOutput.class);
    }

    @SuppressFBWarnings("EI")
    private record TriggerRequest(
        String workflowId, String workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, @Nullable String webhookUrl,
        long environmentId) {
    }

    @SuppressFBWarnings("EI")
    private record TriggerEnableUndoRequest(
        String workflowId, String workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, WebhookEnableOutput enableOutput,
        @Nullable Object previousTriggerState) {
    }
}
