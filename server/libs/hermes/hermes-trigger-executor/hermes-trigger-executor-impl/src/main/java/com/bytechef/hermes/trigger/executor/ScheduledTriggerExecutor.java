
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

package com.bytechef.hermes.trigger.executor;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.service.TriggerExecutionService;
import com.bytechef.hermes.service.TriggerLifecycleService;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service
public class ScheduledTriggerExecutor {

    private final MessageBroker messageBroker;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerLifecycleService triggerLifecycleService;

    public ScheduledTriggerExecutor(
        MessageBroker messageBroker, TriggerExecutionService triggerExecutionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerLifecycleService triggerLifecycleService) {

        this.messageBroker = messageBroker;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerLifecycleService = triggerLifecycleService;
    }

    public LocalDateTime executeTriggerDynamicWebhookRefresh(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId) {

        LocalDateTime webhookExpirationDate = null;

        TriggerDefinition.DynamicWebhookEnableOutput output = OptionalUtils.get(
            triggerLifecycleService.fetchValue(workflowExecutionId.getInstanceId(), workflowExecutionId.toString()));

        output = triggerDefinitionFacade.executeDynamicWebhookRefresh(
            workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            output);

        if (output != null) {
            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }

    public void executeTriggerPoll(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Map<String, Object> context) {

        TriggerExecution triggerExecution = new TriggerExecution();

        triggerExecution.setStatus(TriggerExecution.Status.CREATED);
        triggerExecution.setWorkflowExecutionId(workflowExecutionId);
        triggerExecution.setWorkflowTrigger(workflowTrigger);

        triggerExecutionService.create(triggerExecution);

        triggerExecution.evaluate(context);

        messageBroker.send(TriggerMessageRoute.TRIGGERS, triggerExecution);
    }
}
