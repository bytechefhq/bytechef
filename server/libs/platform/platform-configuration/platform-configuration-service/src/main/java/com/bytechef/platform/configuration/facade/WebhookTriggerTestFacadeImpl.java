/*
 * Copyright 2025 ByteChef
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
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
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

    private static final String WORKFLOW_ENABLED_CACHE =
        WebhookTriggerTestFacade.class.getName() + ".workflowEnabled";
    private static final String WEBHOOK_ENABLE_OUTPUT_CACHE =
        WebhookTriggerTestFacade.class.getName() + ".webhookEnableOutput";

    private final CacheManager cacheManager;
    private final Evaluator evaluator;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final String webhookUrl;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WebhookTriggerTestFacadeImpl(
        CacheManager cacheManager, Evaluator evaluator, ApplicationProperties applicationProperties,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.cacheManager = cacheManager;
        this.evaluator = evaluator;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void disableTrigger(String workflowId, long environmentId, ModeType type) {
        executeTrigger(workflowId, false, type, environmentId);
    }

    @Override
    public String enableTrigger(String workflowId, long environmentId, ModeType type) {
        try {
            executeTrigger(workflowId, false, type, environmentId);
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to disable trigger for workflowId={}, type={}", workflowId, type, e);
            }
        }

        return executeTrigger(workflowId, true, type, environmentId);
    }

    @Override
    public boolean isWorkflowEnabled(WorkflowExecutionId workflowExecutionId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(WORKFLOW_ENABLED_CACHE));

        Boolean enabled = cache.get(workflowExecutionId.toString(), Boolean.class);

        return enabled != null && enabled;
    }

    @Override
    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, long environmentId) {

        String workflowId = getLatestWorkflowId(
            workflowExecutionId.getWorkflowUuid(), workflowExecutionId.getType());

        WorkflowTrigger workflowTrigger = getWorkflowTrigger(workflowId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());
        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, environmentId),
            evaluator);
        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName(), environmentId)
            .orElse(null);

        Cache cache = Objects.requireNonNull(cacheManager.getCache(WORKFLOW_ENABLED_CACHE));

        cache.put(workflowExecutionId.toString(), true);

        return triggerDefinitionFacade.executeWebhookValidateOnEnable(
            workflowNodeType.name(), workflowNodeType.version(),
            workflowNodeType.operation(), triggerParameters, webhookRequest, connectionId);
    }

    private String executeTrigger(String workflowId, boolean enable, ModeType type, long environmentId) {
        String workflowUuid = getWorkflowUuid(workflowId, type);

        WorkflowTrigger workflowTrigger = getWorkflowTrigger(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            type, -1, workflowUuid, workflowTrigger.getName());
        WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());
        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, environmentId), evaluator);
        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(
                workflowId, workflowTrigger.getName(), environmentId)
            .orElse(null);

        Cache cache = Objects.requireNonNull(cacheManager.getCache(WORKFLOW_ENABLED_CACHE));

        if (enable) {
            executeTriggerEnable(
                workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId, environmentId);

            WebhookTriggerFlags webhookTriggerFlags = triggerDefinitionService.getWebhookTriggerFlags(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(), triggerWorkflowNodeType.operation());

            if (!webhookTriggerFlags.workflowSyncOnEnableValidation()) {
                cache.put(workflowExecutionId.toString(), true);
            }
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

        return getWebhookUrl(workflowExecutionId, environmentId);
    }

    private void executeTriggerDisable(
        WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
            triggerWorkflowNodeType.operation());

        switch (triggerDefinition.getType()) {
            case HYBRID, DYNAMIC_WEBHOOK -> {
                Cache cache = Objects.requireNonNull(cacheManager.getCache(WEBHOOK_ENABLE_OUTPUT_CACHE));

                triggerDefinitionFacade.executeWebhookDisable(
                    triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                    triggerWorkflowNodeType.operation(), triggerParameters, workflowExecutionId.toString(),
                    cache.get(workflowExecutionId.toString(), (Callable<Map<String, ?>>) Map::of), connectionId);
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                triggerWorkflowNodeType.operation(), triggerParameters, workflowExecutionId.toString(),
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
        Map<String, ?> triggerParameters, Long connectionId, long environmentId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
            triggerWorkflowNodeType.operation());

        switch (triggerDefinition.getType()) {
            case DYNAMIC_WEBHOOK, HYBRID, STATIC_WEBHOOK -> {
                WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeWebhookEnable(
                    triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                    triggerWorkflowNodeType.operation(), triggerParameters,
                    workflowExecutionId.toString(), connectionId, getWebhookUrl(workflowExecutionId, environmentId));

                if (webhookEnableOutput != null) {
                    Cache cache = Objects.requireNonNull(cacheManager.getCache(WEBHOOK_ENABLE_OUTPUT_CACHE));

                    cache.put(workflowExecutionId.toString(), webhookEnableOutput.parameters());
                }
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                triggerWorkflowNodeType.operation(), triggerParameters, workflowExecutionId.toString(),
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

    private String getLatestWorkflowId(String workflowUuid, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        return jobPrincipalAccessor.getLastWorkflowId(workflowUuid);
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId, long environmentId) {
        return "%s/test/environments/%s".formatted(webhookUrl, environmentId)
            .replace("{id}", workflowExecutionId.toString());
    }

    private String getWorkflowUuid(String workflowId, ModeType type) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        return jobPrincipalAccessor.getWorkflowUuid(workflowId);
    }

    private WorkflowTrigger getWorkflowTrigger(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.of(workflow)
            .getFirst();
    }
}
