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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    @SuppressFBWarnings("EI")
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
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
            triggerWorkflowNodeType.componentOperationName());

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                Map<String, ?> parameters = OptionalUtils.mapOrElse(
                    triggerStateService.fetchValue(workflowExecutionId),
                    WebhookEnableOutput::parameters, Map.of());

                triggerDefinitionFacade.executeWebhookDisable(
                    triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                    triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                    parameters, connectionId);

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
                triggerStateService.delete(workflowExecutionId);
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters,
                workflowExecutionId.toString(), connectionId);
            case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            default -> {
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} disabled",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
        }
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
            triggerWorkflowNodeType.componentOperationName());

        switch (triggerDefinition.getType()) {
            case DYNAMIC_WEBHOOK, HYBRID, STATIC_WEBHOOK -> {
                WebhookEnableOutput output =
                    triggerDefinitionFacade.executeWebhookEnable(
                        triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                        triggerWorkflowNodeType.componentOperationName(), triggerParameters,
                        workflowExecutionId.toString(), connectionId, webhookUrl);

                if (output != null) {
                    triggerStateService.save(workflowExecutionId, output);

                    if (output.webhookExpirationDate() != null) {
                        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                            output.webhookExpirationDate(), triggerWorkflowNodeType.componentName(),
                            triggerWorkflowNodeType.componentVersion(), workflowExecutionId);
                    }
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                connectionId);
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> {
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} enabled",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
        }
    }
}
