
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

package com.bytechef.hermes.scheduler.handler;

import com.bytechef.commons.util.InstantUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.scheduler.trigger.constant.TriggerSchedulerConstants;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
public class DynamicWebhookTriggerRefreshTaskVoidExecutionHandler
    implements VoidExecutionHandler<WorkflowExecutionId> {

    private final ApplicationContext applicationContext;

    public DynamicWebhookTriggerRefreshTaskVoidExecutionHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void execute(TaskInstance<WorkflowExecutionId> taskInstance, ExecutionContext executionContext) {
        WorkflowExecutionId workflowExecutionId = taskInstance.getData();

        LocalDateTime webhookExpirationDate = refreshDynamicWebhookTrigger(
            workflowExecutionId, workflowExecutionId.getComponentName(),
            workflowExecutionId.getComponentVersion());

        if (webhookExpirationDate != null) {
            SchedulerClient schedulerClient = executionContext.getSchedulerClient();

            schedulerClient.reschedule(
                TriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK.instance(
                    workflowExecutionId.toString(), workflowExecutionId),
                InstantUtils.getInstant(webhookExpirationDate));
        }
    }

    private LocalDateTime refreshDynamicWebhookTrigger(
        WorkflowExecutionId workflowExecutionId, String componentName, int componentVersion) {

        LocalDateTime webhookExpirationDate = null;

        TriggerStateService triggerStateService = applicationContext.getBean(TriggerStateService.class);

        TriggerDefinition.DynamicWebhookEnableOutput output = OptionalUtils.get(
            triggerStateService.fetchValue(workflowExecutionId));

        TriggerDefinitionService triggerDefinitionService = applicationContext.getBean(
            TriggerDefinitionService.class);

        output = triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, workflowExecutionId.getComponentTriggerName(), output);

        if (output != null) {
            triggerStateService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }
}
