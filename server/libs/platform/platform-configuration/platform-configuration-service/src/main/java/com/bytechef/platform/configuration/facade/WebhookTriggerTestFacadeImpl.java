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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WebhookTriggerTestFacadeImpl implements WebhookTriggerTestFacade {

    private static final Logger log = LoggerFactory.getLogger(WebhookTriggerTestFacadeImpl.class);

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final String publicUrl;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    public WebhookTriggerTestFacadeImpl(
        ApplicationProperties applicationProperties,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.publicUrl = applicationProperties.getPublicUrl();
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
    }

    @Override
    public void disableTrigger(String workflowId, ModeType type) {
        executeTrigger(false, workflowId, type);
    }

    @Override
    public String enableTrigger(String workflowId, ModeType type) {
        try {
            executeTrigger(false, workflowId, type);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to disable trigger for workflowId={}, type={}", workflowId, type, e);
            }
        }

        return executeTrigger(true, workflowId, type);
    }

    @Override
    public void saveTestOutput(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

    }

    @Override
    public void stopByWorkflowReferenceCode(String workflowReferenceCode, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        executeTrigger(false, jobPrincipalAccessor.getLatestWorkflowId(workflowReferenceCode), type);
    }

    private String executeTrigger(boolean enable, String workflowId, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        String workflowReferenceCode = jobPrincipalAccessor.getWorkflowReferenceCode(workflowId);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflow)
            .getFirst();

        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName())
            .orElse(null);
        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId));
        WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            type, -1, workflowReferenceCode, workflowTrigger.getName());

        if (enable) {
            executeTriggerEnable(workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId);
        } else {
            executeTriggerDisable(workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId);
        }

        return getWebhookUrl(workflowExecutionId);
    }

    private void executeTriggerDisable(
        WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
            triggerWorkflowNodeType.componentOperationName());

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                triggerDefinitionFacade.executeWebhookDisable(
                    triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                    triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                    Map.of(), connectionId);
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters,
                workflowExecutionId.toString(), connectionId);
            default -> {
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} disabled",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
        }
    }

    private void executeTriggerEnable(
        WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
            triggerWorkflowNodeType.componentOperationName());

        switch (triggerDefinition.getType()) {
            case DYNAMIC_WEBHOOK, HYBRID, STATIC_WEBHOOK -> triggerDefinitionFacade.executeWebhookEnable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters,
                workflowExecutionId.toString(), connectionId, getWebhookUrl(workflowExecutionId));
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                connectionId);
            default -> {
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} enabled",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
        }
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return "%s/webhooks/%s/test".formatted(publicUrl, workflowExecutionId.toString());
    }
}
