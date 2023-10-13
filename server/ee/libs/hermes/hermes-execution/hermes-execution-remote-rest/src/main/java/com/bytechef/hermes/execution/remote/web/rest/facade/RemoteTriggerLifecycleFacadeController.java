/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.web.rest.facade;

import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
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
    public void executeTriggerDisable(TriggerRequest triggerRequest) {
        triggerLifecycleFacade.executeTriggerDisable(
            triggerRequest.workflowId, triggerRequest.instanceId, triggerRequest.instanceType,
            triggerRequest.workflowTriggerName, triggerRequest.workflowTriggerType, triggerRequest.triggerParameters,
            triggerRequest.connectionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-lifecycle-facade/execute-trigger-enable")
    public void executeTriggerEnable(TriggerRequest triggerRequest) {
        triggerLifecycleFacade.executeTriggerEnable(
            triggerRequest.workflowId, triggerRequest.instanceId, triggerRequest.instanceType,
            triggerRequest.workflowTriggerName, triggerRequest.workflowTriggerType, triggerRequest.triggerParameters,
            triggerRequest.connectionId, triggerRequest.webhookUrl);
    }

    @SuppressFBWarnings("EI")
    public record TriggerRequest(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId, String webhookUrl) {
    }
}
