
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

package com.bytechef.hermes.scheduler.trigger.executor;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.execution.service.TriggerLifecycleService;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerExecutor {

    private final MessageBroker messageBroker;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerLifecycleService triggerLifecycleService;

    public TriggerSchedulerExecutor(
        MessageBroker messageBroker, TriggerDefinitionService triggerDefinitionService,
        TriggerLifecycleService triggerLifecycleService) {

        this.messageBroker = messageBroker;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerLifecycleService = triggerLifecycleService;
    }

    public void poll(WorkflowExecutionId workflowExecutionId) {
        messageBroker.send(TriggerMessageRoute.TRIGGERS_POLLS, workflowExecutionId);
    }

    public LocalDateTime refreshDynamicWebhook(
        WorkflowExecutionId workflowExecutionId, String componentName, int componentVersion) {

        LocalDateTime webhookExpirationDate = null;

        DynamicWebhookEnableOutput output = OptionalUtils.get(
            triggerLifecycleService.fetchValue(workflowExecutionId));

        output = triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, workflowExecutionId.getTriggerName(), output);

        if (output != null) {
            triggerLifecycleService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }

    public void triggerWorkflow(WorkflowExecutionId workflowExecutionId, Map<String, Object> output) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .output(output)
            .workflowExecutionId(workflowExecutionId)
            .build();

        messageBroker.send(TriggerMessageRoute.TRIGGERS_COMPLETIONS, triggerExecution);
    }
}
