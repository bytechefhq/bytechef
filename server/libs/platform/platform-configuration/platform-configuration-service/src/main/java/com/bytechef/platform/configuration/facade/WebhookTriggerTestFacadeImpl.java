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
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WebhookTriggerTestFacadeImpl implements WebhookTriggerTestFacade {

    private static final Logger log = LoggerFactory.getLogger(WebhookTriggerTestFacadeImpl.class);

    private final CacheManager cacheManager;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final String webhookUrl;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WebhookTriggerTestFacadeImpl(
        CacheManager cacheManager, ApplicationProperties applicationProperties,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.cacheManager = cacheManager;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void disableTrigger(String workflowId, ModeType type) {
        executeTrigger(workflowId, type, false);
    }

    @Override
    public String enableTrigger(String workflowId, ModeType type) {
        try {
            executeTrigger(workflowId, type, false);
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to disable trigger for workflowId={}, type={}", workflowId, type, e);
            }
        }

        return executeTrigger(workflowId, type, true);
    }

    @Override
    public boolean isWorkflowEnabled(WorkflowExecutionId workflowExecutionId) {
        Cache cache = getWebhookTriggerTestsCache();

        Boolean enabled = cache.get(workflowExecutionId.toString(), Boolean.class);

        return enabled != null && enabled;
    }

    @Override
    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        String workflowId = getLatestWorkflowId(
            workflowExecutionId.getWorkflowReferenceCode(), workflowExecutionId.getType());

        WorkflowTrigger workflowTrigger = getWorkflowTrigger(workflowId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());
        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId));
        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName())
            .orElse(null);

        return triggerDefinitionFacade.executeWebhookValidateOnEnable(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), triggerParameters, webhookRequest, connectionId);
    }

    private String executeTrigger(String workflowId, ModeType type, boolean enable) {
        String workflowReferenceCode = getWorkflowReferenceCode(workflowId, type);

        WorkflowTrigger workflowTrigger = getWorkflowTrigger(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            type, -1, workflowReferenceCode, workflowTrigger.getName());
        WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());
        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId));
        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName())
            .orElse(null);

        Cache cache = getWebhookTriggerTestsCache();

        if (enable) {
            executeTriggerEnable(workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId);

            cache.putIfAbsent(workflowExecutionId.toString(), true);
        } else {
            try {
                executeTriggerDisable(workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId);
            } catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to disable trigger for workflowId={}, type={}", workflowId, type, e);
                }
            }

            cache.evictIfPresent(workflowExecutionId.toString());
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
                Cache cache = getWebhookEnableOutputCache();

                triggerDefinitionFacade.executeWebhookDisable(
                    triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                    triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                    cache.get(workflowExecutionId.toString(), (Callable<Map<String, ?>>) Map::of), connectionId);
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName(), triggerParameters, workflowExecutionId.toString(),
                connectionId);
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
            case DYNAMIC_WEBHOOK, HYBRID, STATIC_WEBHOOK -> {
                WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeWebhookEnable(
                    triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                    triggerWorkflowNodeType.componentOperationName(), triggerParameters,
                    workflowExecutionId.toString(), connectionId, getWebhookUrl(workflowExecutionId));

                if (webhookEnableOutput != null) {
                    Cache cache = getWebhookEnableOutputCache();

                    cache.put(workflowExecutionId.toString(), webhookEnableOutput.parameters());
                }
            }
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

    private String getLatestWorkflowId(String workflowReferenceCode, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        return jobPrincipalAccessor.getLatestWorkflowId(workflowReferenceCode);
    }

    private Cache getWebhookEnableOutputCache() {
        return cacheManager.getCache(WebhookTriggerTestFacade.class + ".webhookEnableOutputs");
    }

    private Cache getWebhookTriggerTestsCache() {
        return cacheManager.getCache(WebhookTriggerTestFacade.class + "webhooks");
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return "%s/test".formatted(webhookUrl)
            .replace("{id}", workflowExecutionId.toString());
    }

    private String getWorkflowReferenceCode(String workflowId, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        return jobPrincipalAccessor.getWorkflowReferenceCode(workflowId);
    }

    private WorkflowTrigger getWorkflowTrigger(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.of(workflow)
            .getFirst();
    }
}
