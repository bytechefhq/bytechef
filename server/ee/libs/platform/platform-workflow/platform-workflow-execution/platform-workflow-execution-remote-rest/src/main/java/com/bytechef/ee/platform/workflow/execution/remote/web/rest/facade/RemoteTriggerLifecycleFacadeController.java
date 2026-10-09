/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/trigger-lifecycle-facade")
public class RemoteTriggerLifecycleFacadeController {

    private final TriggerLifecycleFacade triggerLifecycleFacade;

    @SuppressFBWarnings("EI")
    public RemoteTriggerLifecycleFacadeController(TriggerLifecycleFacade triggerLifecycleFacade) {
        this.triggerLifecycleFacade = triggerLifecycleFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-trigger-disable")
    public boolean executeTriggerDisable(@RequestBody TriggerRequest triggerRequest) {
        return triggerLifecycleFacade.executeTriggerDisable(
            triggerRequest.workflowId, WorkflowExecutionId.parse(triggerRequest.workflowExecutionId),
            triggerRequest.triggerWorkflowNodeType,
            triggerRequest.triggerParameters, triggerRequest.connectionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-trigger-enable-undo")
    public boolean executeTriggerEnableUndo(@RequestBody TriggerEnableUndoRequest triggerEnableUndoRequest) {
        return triggerLifecycleFacade.executeTriggerEnableUndo(
            triggerEnableUndoRequest.workflowId,
            WorkflowExecutionId.parse(triggerEnableUndoRequest.workflowExecutionId),
            triggerEnableUndoRequest.triggerWorkflowNodeType, triggerEnableUndoRequest.triggerParameters,
            triggerEnableUndoRequest.connectionId, triggerEnableUndoRequest.enableOutput,
            triggerEnableUndoRequest.previousTriggerState);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-trigger-enable")
    public @Nullable WebhookEnableOutput executeTriggerEnable(@RequestBody TriggerRequest triggerRequest) {
        return triggerLifecycleFacade.executeTriggerEnable(
            triggerRequest.workflowId, WorkflowExecutionId.parse(triggerRequest.workflowExecutionId),
            triggerRequest.triggerWorkflowNodeType,
            triggerRequest.triggerParameters, triggerRequest.connectionId, triggerRequest.webhookUrl,
            triggerRequest.environmentId);
    }

    @SuppressFBWarnings("EI")
    public record TriggerRequest(
        String workflowId, String workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, @Nullable String webhookUrl,
        long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record TriggerEnableUndoRequest(
        String workflowId, String workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, WebhookEnableOutput enableOutput,
        @Nullable Object previousTriggerState) {
    }
}
