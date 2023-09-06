
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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service
public class TriggerLifecycleFacadeImpl implements TriggerLifecycleFacade {

    private final TriggerScheduler triggerScheduler;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerStateService triggerStateService;

    public TriggerLifecycleFacadeImpl(
        TriggerScheduler triggerScheduler, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerStateService triggerStateService,
        WorkflowService workflowService) {

        this.triggerScheduler = triggerScheduler;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerStateService = triggerStateService;
    }

    @Override
    public void executeTriggerDisable(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId) {

        ComponentOperation componentOperation = ComponentOperation.ofType(workflowTriggerType);

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName());

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            instanceType, instanceId, workflowId, workflowTriggerName);

        DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            triggerStateService.fetchValue(workflowExecutionId), null);

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionFacade.executeDynamicWebhookDisable(
                    componentOperation.componentName(), componentOperation.componentVersion(),
                    componentOperation.operationName(), triggerParameters, workflowExecutionId.toString(),
                    output, connectionId);

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), triggerParameters,
                workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId, String webhookUrl) {

        ComponentOperation componentOperation = ComponentOperation.ofType(workflowTriggerType);

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName());

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            instanceType, instanceId, workflowId, workflowTriggerName);

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                DynamicWebhookEnableOutput output =
                    triggerDefinitionFacade.executeDynamicWebhookEnable(
                        componentOperation.componentName(), componentOperation.componentVersion(),
                        componentOperation.operationName(), triggerParameters,
                        workflowExecutionId.toString(), connectionId, webhookUrl);

                if (output != null) {
                    triggerStateService.save(workflowExecutionId, output);

                    if (output.webhookExpirationDate() != null) {
                        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                            output.webhookExpirationDate(), componentOperation.componentName(),
                            componentOperation.componentVersion(), workflowExecutionId);
                    }
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), triggerParameters, workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }
}
