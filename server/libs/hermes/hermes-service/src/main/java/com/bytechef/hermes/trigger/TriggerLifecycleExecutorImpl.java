
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

package com.bytechef.hermes.trigger;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.scheduler.TaskScheduler;
import com.bytechef.hermes.service.TriggerLifecycleService;
import com.bytechef.hermes.workflow.WorkflowExecutionId;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerLifecycleExecutorImpl implements TriggerLifecycleExecutor {

    private final TaskScheduler taskScheduler;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerLifecycleService triggerLifecycleService;
    private final String webhookUrl;

    public TriggerLifecycleExecutorImpl(
        TaskScheduler taskScheduler, TriggerDefinitionService triggerDefinitionService,
        TriggerLifecycleService triggerLifecycleService, String webhookUrl) {

        this.taskScheduler = taskScheduler;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerLifecycleService = triggerLifecycleService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void executeTriggerDisable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection) {

        DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            triggerLifecycleService.fetchValue(
                workflowExecutionId.getInstanceId(), workflowExecutionId.toString()),
            null);
        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
            workflowTrigger.getComponentVersion());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionService.executeDynamicWebhookDisable(
                    workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
                    workflowTrigger.getComponentVersion(),
                    connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    workflowExecutionId.toString(), output);

                taskScheduler.cancelRefreshDynamicWebhookTriggerTask(workflowExecutionId.toString());
            }
            case LISTENER -> triggerDefinitionService.executeListenerDisable(
                workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
                workflowTrigger.getComponentVersion(),
                connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> taskScheduler.cancelPollTriggerTask(workflowExecutionId.toString());
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    @Override
    public void executeTriggerEnable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection) {

        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
            workflowTrigger.getComponentVersion());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                DynamicWebhookEnableOutput output = triggerDefinitionService.executeDynamicWebhookEnable(
                    workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
                    workflowTrigger.getComponentVersion(),
                    connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    createWebhookUrl(workflowExecutionId), workflowExecutionId.toString());

                if (output != null) {
                    triggerLifecycleService.save(
                        workflowExecutionId.getInstanceId(), workflowExecutionId.toString(), output);

                    if (output.webhookExpirationDate() != null) {
                        taskScheduler.scheduleRefreshDynamicWebhookTriggerTask(
                            workflowExecutionId, output.webhookExpirationDate(), workflowTrigger.getComponentName(),
                            workflowTrigger.getComponentVersion());
                    }
                }
            }
            case LISTENER -> triggerDefinitionService.executeListenerEnable(
                workflowTrigger.getTriggerName(), workflowTrigger.getComponentName(),
                workflowTrigger.getComponentVersion(),
                connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> taskScheduler.schedulePollTriggerTask(workflowExecutionId);
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    private String createWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }
}
