/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TriggerLifecycleFacadeImpl implements TriggerLifecycleFacade {

    private static final Logger logger = LoggerFactory.getLogger(TriggerLifecycleFacadeImpl.class);

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

    @Override
    public void executeTriggerDisable(
        String workflowId, int type, long instanceId, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId) {

        OperationType operationType = OperationType.ofType(workflowTriggerType);

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            operationType.componentName(), operationType.componentVersion(),
            operationType.componentOperationName());

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            type, instanceId, workflowId, workflowTriggerName);

        DynamicWebhookEnableOutput output = OptionalUtils.orElse(
            triggerStateService.fetchValue(workflowExecutionId), null);

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionFacade.executeDynamicWebhookDisable(
                    operationType.componentName(), operationType.componentVersion(),
                    operationType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                    output.parameters(), connectionId);

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                operationType.componentName(), operationType.componentVersion(),
                operationType.componentOperationName(), triggerParameters,
                workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            default -> {
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} disabled",
                workflowTriggerType, workflowTriggerName, workflowExecutionId);
        }
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, int type, long instanceId, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl) {

        OperationType operationType = OperationType.ofType(workflowTriggerType);

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            operationType.componentName(), operationType.componentVersion(),
            operationType.componentOperationName());

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            type, instanceId, workflowId, workflowTriggerName);

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                DynamicWebhookEnableOutput output =
                    triggerDefinitionFacade.executeDynamicWebhookEnable(
                        operationType.componentName(), operationType.componentVersion(),
                        operationType.componentOperationName(), triggerParameters,
                        workflowExecutionId.toString(), connectionId, webhookUrl);

                if (output != null) {
                    triggerStateService.save(workflowExecutionId, output);

                    if (output.webhookExpirationDate() != null) {
                        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                            output.webhookExpirationDate(), operationType.componentName(),
                            operationType.componentVersion(), workflowExecutionId);
                    }
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                operationType.componentName(), operationType.componentVersion(),
                operationType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                connectionId);
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> {
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} enabled",
                workflowTriggerType, workflowTriggerName, workflowExecutionId);
        }
    }
}
