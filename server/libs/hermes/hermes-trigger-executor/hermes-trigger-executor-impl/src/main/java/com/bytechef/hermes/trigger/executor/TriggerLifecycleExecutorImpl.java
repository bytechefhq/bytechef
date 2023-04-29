
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

import com.bytechef.commons.util.InstantUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.data.storage.domain.DataStorage;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.trigger.executor.data.TriggerScheduleAndData;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

import static com.bytechef.hermes.trigger.executor.constants.TriggerScheduleConstants.TRIGGER_POLL_RECURRING_TASK;
import static com.bytechef.hermes.trigger.executor.constants.TriggerScheduleConstants.TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK;

/**
 * @author Ivica Cardic
 */
@Service
public class TriggerLifecycleExecutorImpl implements TriggerLifecycleExecutor {

    private final DataStorageService dataStorageService;
    private final SchedulerClient schedulerClient;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;

    public TriggerLifecycleExecutorImpl(
        DataStorageService dataStorageService, SchedulerClient schedulerClient,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        @Value("bytechef.webhookUrl") String webhookUrl) {

        this.dataStorageService = dataStorageService;
        this.schedulerClient = schedulerClient;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void executeTriggerDisable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection,
        Map<String, Object> context) {

        DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            dataStorageService.fetchValue(
                DataStorage.Scope.WORKFLOW_INSTANCE, workflowExecutionId.getInstanceId(),
                workflowExecutionId.toString()),
            null);
        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID_DYNAMIC, WEBHOOK_DYNAMIC -> {
                triggerDefinitionFacade.executeDynamicWebhookDisable(
                    workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                    workflowTrigger.getTriggerName(), connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    workflowExecutionId.toString(), output);

                schedulerClient.cancel(
                    TaskInstanceId.of(
                        TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.getTaskName(), workflowExecutionId.toString()));
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getTriggerName(), connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> schedulerClient.cancel(
                TaskInstanceId.of(
                    TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.getTaskName(), workflowExecutionId.toString()));
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    @Override
    public void executeTriggerEnable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection,
        Map<String, Object> context) {

        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID_DYNAMIC, WEBHOOK_DYNAMIC -> {
                DynamicWebhookEnableOutput output = triggerDefinitionFacade.executeDynamicWebhookEnable(
                    workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                    workflowTrigger.getTriggerName(), connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    createWebhookUrl(workflowExecutionId), workflowExecutionId.toString());

                if (output != null) {
                    dataStorageService.save(
                        DataStorage.Scope.WORKFLOW_INSTANCE, workflowExecutionId.getInstanceId(),
                        workflowExecutionId.toString(), output);

                    if (output.webhookExpirationDate() != null) {
                        schedulerClient.schedule(
                            TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.instance(
                                workflowExecutionId.toString(),
                                new TriggerScheduleAndData(workflowExecutionId, workflowTrigger)),
                            InstantUtils.getInstant(output.webhookExpirationDate()));
                    }
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getTriggerName(), connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> {
                Schedule schedule = FixedDelay.ofMinutes(5);

                schedulerClient.schedule(
                    TRIGGER_POLL_RECURRING_TASK.instance(
                        workflowExecutionId.toString(),
                        new TriggerScheduleAndData(schedule, workflowExecutionId, workflowTrigger, context)),
                    schedule.getInitialExecutionTime(Instant.now()));
            }
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    private String createWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }
}
