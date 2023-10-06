
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.execution.remote.client.facade;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerLifecycleFacadeClient implements TriggerLifecycleFacade {

    private static final String TRIGGER_LIFECYCLE_FACADE = "/remote/trigger-lifecycle-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerLifecycleFacadeClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public void executeTriggerDisable(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable",
            new TriggerRequest(
                workflowId, instanceId, instanceType, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, null));
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable",
            new TriggerRequest(
                workflowId, instanceId, instanceType, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, webhookUrl));
    }

    private void post(String path, TriggerRequest workflowExecutionId) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(path)
                .build(),
            workflowExecutionId);
    }

    @SuppressFBWarnings("EI")
    private record TriggerRequest(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId, String webhookUrl) {
    }
}
