
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

package com.bytechef.hermes.execution.remote.web.rest.facade;

import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
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
