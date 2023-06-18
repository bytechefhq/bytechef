
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

package com.bytechef.hermes.execution.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import com.bytechef.hermes.execution.WorkflowExecutionId;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerLifecycleFacadeImpl implements TriggerLifecycleFacade {

    private final TriggerScheduler triggerScheduler;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerStateService triggerStateService;
    private final String webhookUrl;

    public TriggerLifecycleFacadeImpl(
        TriggerScheduler triggerScheduler, TriggerDefinitionService triggerDefinitionService,
        TriggerStateService triggerStateService, String webhookUrl) {

        this.triggerScheduler = triggerScheduler;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerStateService = triggerStateService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void executeTriggerDisable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection) {

        DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            triggerStateService.fetchValue(workflowExecutionId), null);
        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionService.executeDynamicWebhookDisable(
                    workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                    workflowTrigger.getTriggerName(),
                    connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    workflowExecutionId.toString(), output);

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
            }
            case LISTENER -> triggerDefinitionService.executeListenerDisable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getTriggerName(),
                connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    @Override
    public void executeTriggerEnable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, Connection connection) {

        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                DynamicWebhookEnableOutput output = triggerDefinitionService.executeDynamicWebhookEnable(
                    workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                    workflowTrigger.getTriggerName(),
                    connection == null ? Map.of() : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                    createWebhookUrl(workflowExecutionId), workflowExecutionId.toString());

                if (output != null) {
                    triggerStateService.save(workflowExecutionId, output);

                    if (output.webhookExpirationDate() != null) {
                        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                            output.webhookExpirationDate(), workflowTrigger.getComponentName(),
                            workflowTrigger.getComponentVersion(), workflowExecutionId);
                    }
                }
            }
            case LISTENER -> triggerDefinitionService.executeListenerEnable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getTriggerName(),
                connection == null ? Map.of() : connection.getParameters(),
                connection == null ? null : connection.getAuthorizationName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString());
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    private String createWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }
}
