/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.facade;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
    public void executeTriggerDisable(
        String workflowId, PlatformType type, long instanceId, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable",
            new TriggerRequest(
                workflowId, instanceId, type, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, null));
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, PlatformType type, long instanceId, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable",
            new TriggerRequest(
                workflowId, instanceId, type, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, webhookUrl));
    }

    private void post(String path, TriggerRequest workflowExecutionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(path)
                .build(),
            workflowExecutionId);
    }

    @SuppressFBWarnings("EI")
    private record TriggerRequest(
        String workflowId, long instanceId, PlatformType type, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId, String webhookUrl) {
    }
}
