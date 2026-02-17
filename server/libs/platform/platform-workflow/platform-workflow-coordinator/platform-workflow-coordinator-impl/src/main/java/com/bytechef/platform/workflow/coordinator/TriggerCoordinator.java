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

package com.bytechef.platform.workflow.coordinator;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.ApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.ErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionCompleteEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.platform.workflow.coordinator.event.listener.ErrorEventListener;
import com.bytechef.platform.workflow.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCoordinator.class);

    private final List<ApplicationEventListener> applicationEventListeners;
    private final List<ErrorEventListener> errorEventListeners;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerCompletionHandler triggerCompletionHandler;
    private final TriggerDispatcher triggerDispatcher;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerScheduler triggerScheduler;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCoordinator(
        List<ApplicationEventListener> applicationEventListeners, List<ErrorEventListener> errorEventListeners,
        Evaluator evaluator, ApplicationEventPublisher eventPublisher,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerCompletionHandler triggerCompletionHandler,
        TriggerDispatcher triggerDispatcher, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, TriggerScheduler triggerScheduler,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        this.applicationEventListeners = applicationEventListeners;
        this.errorEventListeners = errorEventListeners;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.triggerCompletionHandler = triggerCompletionHandler;
        this.triggerDispatcher = triggerDispatcher;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerScheduler = triggerScheduler;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    /**
     *
     * @param applicationEvent
     */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onApplicationEvent: applicationEvent={}", applicationEvent);
        }

        for (ApplicationEventListener applicationEventListener : applicationEventListeners) {
            applicationEventListener.onApplicationEvent(applicationEvent);
        }
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onErrorEvent: errorEvent={}", errorEvent);
        }

        for (ErrorEventListener errorEventListener : errorEventListeners) {
            errorEventListener.onErrorEvent(errorEvent);
        }
    }

    /**
     * Complete a trigger of a given workflow execution.
     *
     * @param triggerExecutionCompleteEvent The trigger execution event to complete.
     */
    // TODO @Transactional
    public void onTriggerExecutionCompleteEvent(TriggerExecutionCompleteEvent triggerExecutionCompleteEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace(
                "onTriggerExecutionCompleteEvent: triggerExecutionCompleteEvent={}", triggerExecutionCompleteEvent);
        }

        handleTriggerExecutionCompletion(triggerExecutionCompleteEvent.getTriggerExecution());
    }

    // TODO @Transactional
    public void onTriggerListenerEvent(TriggerListenerEvent triggerListenerEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTriggerListenerEvent: triggerListenerEvent={}", triggerListenerEvent);
        }

        WorkflowExecutionId workflowExecutionId = triggerListenerEvent.getWorkflowExecutionId();

        TenantContext.runWithTenantId(workflowExecutionId.getTenantId(), () -> {
            try {
                TriggerExecution triggerExecution = TriggerExecution.builder()
                    .startDate(triggerListenerEvent.getExecutionDate())
                    .endDate(triggerListenerEvent.getExecutionDate())
                    .workflowExecutionId(workflowExecutionId)
                    .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
                    .build();

                triggerExecution = triggerExecutionService.create(triggerExecution);

                triggerExecution.setOutput(
                    triggerFileStorage.storeTriggerExecutionOutput(
                        Validate.notNull(triggerExecution.getId(), "id"), triggerListenerEvent.getOutput()));

                handleTriggerExecutionCompletion(triggerExecution);
            } catch (IllegalArgumentException | ConfigurationException exception) {
                logger.warn(
                    "Cancelling orphaned schedule trigger for workflowExecutionId='{}': {}",
                    workflowExecutionId, exception.getMessage());

                triggerScheduler.cancelScheduleTrigger(workflowExecutionId.toString());
            }
        });
    }

    /**
     * Invoked every poll interval (5 mins by default) for a given workflow execution to dispatch request for trigger's
     * handler execution.
     *
     * @param triggerPollEvent The trigger poll event.
     */
    // TODO @Transactional
    public void onTriggerPollEvent(TriggerPollEvent triggerPollEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTriggerPollEvent: triggerPollEvent={}", triggerPollEvent);
        }

        WorkflowExecutionId workflowExecutionId = triggerPollEvent.getWorkflowExecutionId();

        TenantContext.runWithTenantId(workflowExecutionId.getTenantId(), () -> {
            try {
                TriggerExecution triggerExecution = TriggerExecution.builder()
                    .workflowExecutionId(workflowExecutionId)
                    .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
                    .build();

                dispatch(triggerExecution);

                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Poll trigger id={}, type='{}', name='{}', workflowExecutionId='{}' dispatched",
                        triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName(),
                        triggerExecution.getWorkflowExecutionId());
                }
            } catch (IllegalArgumentException | ConfigurationException exception) {
                logger.warn(
                    "Cancelling orphaned polling trigger for workflowExecutionId='{}': {}",
                    workflowExecutionId, exception.getMessage());

                triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
            }
        });
    }

    /**
     *
     * @param triggerWebhookEvent
     */
    // TODO @Transactional
    public void onTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTriggerWebhookEvent: triggerWebhookEvent={}", triggerWebhookEvent);
        }

        WorkflowExecutionId workflowExecutionId = triggerWebhookEvent.getWorkflowExecutionId();

        TenantContext.runWithTenantId(workflowExecutionId.getTenantId(), () -> {
            try {
                TriggerExecution triggerExecution = TriggerExecution.builder()
                    .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, triggerWebhookEvent.getWebhookRequest()))
                    .workflowExecutionId(workflowExecutionId)
                    .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
                    .build();

                dispatch(triggerExecution);

                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Webhook trigger id={}, type='{}', name='{}', workflowExecutionId='{}' dispatched",
                        triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName(),
                        triggerExecution.getWorkflowExecutionId());
                }
            } catch (IllegalArgumentException | ConfigurationException exception) {
                logger.warn(
                    "Cancelling orphaned dynamic webhook trigger refresh for workflowExecutionId='{}': {}",
                    workflowExecutionId, exception.getMessage());

                triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
            }
        });
    }

    private void dispatch(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        triggerExecution = triggerExecutionService.create(
            triggerExecution.evaluate(
                jobPrincipalAccessor.getInputMap(
                    workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid()),
                evaluator));

        triggerExecution.setState(
            triggerStateService.fetchValue(workflowExecutionId)
                .orElse(null));

        try {
            triggerDispatcher.dispatch(triggerExecution);
        } catch (Exception e) {
            publishTriggerError(triggerExecution, e);
        }
    }

    private void handleTriggerExecutionCompletion(TriggerExecution triggerExecution) {
        try {
            triggerCompletionHandler.handle(triggerExecution);
        } catch (Exception e) {
            publishTriggerError(triggerExecution, e);
        }
    }

    private void publishTriggerError(TriggerExecution triggerExecution, Exception e) {
        triggerExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

        eventPublisher.publishEvent(new TriggerExecutionErrorEvent(triggerExecution));
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getTriggerName()));
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }
}
