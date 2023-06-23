
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
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import org.springframework.stereotype.Service;

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
        TriggerDefinitionService triggerDefinitionService, TriggerStateService triggerStateService) {

        this.triggerScheduler = triggerScheduler;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerStateService = triggerStateService;
    }

    public void executeTriggerDisable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, long connectionId) {

        TriggerDefinition.DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            triggerStateService.fetchValue(workflowExecutionId), null);
        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getComponentTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionFacade.executeDynamicWebhookDisable(
                    workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                    workflowTrigger.getComponentTriggerName(), workflowTrigger.getParameters(),
                    workflowExecutionId.toString(), output, connectionId);

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getComponentTriggerName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    public void executeTriggerEnable(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, long connectionId) {

        TriggerDefinitionDTO triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
            workflowTrigger.getComponentTriggerName());

        switch (triggerDefinition.type()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                TriggerDefinition.DynamicWebhookEnableOutput output =
                    triggerDefinitionFacade.executeDynamicWebhookEnable(
                        workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                        workflowTrigger.getComponentTriggerName(), workflowTrigger.getParameters(),
                        workflowExecutionId.toString(), connectionId);

                if (output != null) {
                    triggerStateService.save(workflowExecutionId, output);

                    if (output.webhookExpirationDate() != null) {
                        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                            output.webhookExpirationDate(), workflowTrigger.getComponentName(),
                            workflowTrigger.getComponentVersion(), workflowExecutionId);
                    }
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion(),
                workflowTrigger.getComponentTriggerName(), workflowTrigger.getParameters(),
                workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> throw new IllegalArgumentException("Invalid trigger type");
        }
    }
}
