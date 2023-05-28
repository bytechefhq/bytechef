
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

package com.bytechef.hermes.scheduler.executor;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.service.TriggerLifecycleService;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Component
public class ScheduledTriggerExecutor {

    private final MessageBroker messageBroker;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerLifecycleService triggerLifecycleService;

    public ScheduledTriggerExecutor(
        MessageBroker messageBroker, TriggerDefinitionService triggerDefinitionService,
        TriggerLifecycleService triggerLifecycleService) {

        this.messageBroker = messageBroker;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerLifecycleService = triggerLifecycleService;
    }

    public LocalDateTime executeTriggerDynamicWebhookRefresh(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId) {

        LocalDateTime webhookExpirationDate = null;

        DynamicWebhookEnableOutput output = OptionalUtils.get(
            triggerLifecycleService.fetchValue(workflowExecutionId.getInstanceId(), workflowExecutionId.toString()));

        output = triggerDefinitionService.executeDynamicWebhookRefresh(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(), workflowTrigger.getTriggerName(),
            output);

        if (output != null) {
            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }

    public void executeTriggerPoll(WorkflowExecutionId workflowExecutionId) {
        messageBroker.send(TriggerMessageRoute.TRIGGERS_REQUESTS, workflowExecutionId);
    }
}
