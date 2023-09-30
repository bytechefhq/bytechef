
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

package com.bytechef.hermes.coordinator;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.ExceptionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.coordinator.event.ApplicationEvent;
import com.bytechef.hermes.coordinator.event.ErrorEvent;
import com.bytechef.hermes.coordinator.event.TriggerExecutionCompleteEvent;
import com.bytechef.hermes.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.hermes.coordinator.event.TriggerListenerEvent;
import com.bytechef.hermes.coordinator.event.TriggerPollEvent;
import com.bytechef.hermes.coordinator.event.TriggerWebhookEvent;
import com.bytechef.hermes.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.hermes.coordinator.event.listener.ErrorEventListener;
import com.bytechef.hermes.coordinator.event.listener.TriggerExecutionErrorEventListener;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessor;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.RemoteTriggerExecutionService;
import com.bytechef.hermes.execution.service.RemoteTriggerStateService;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCoordinator.class);

    private final List<ApplicationEventListener> applicationEventListeners;
    private final List<ErrorEventListener> errorEventListeners;
    private final ApplicationEventPublisher eventPublisher;
    private final InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry;
    private final TriggerCompletionHandler triggerCompletionHandler;
    private final TriggerDispatcher triggerDispatcher;
    private final TriggerExecutionErrorEventListener triggerExecutionErrorEventListener;
    private final RemoteTriggerExecutionService triggerExecutionService;
    private final RemoteTriggerStateService triggerStateService;
    private final RemoteWorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCoordinator(
        List<ApplicationEventListener> applicationEventListeners, List<ErrorEventListener> errorEventListeners,
        ApplicationEventPublisher eventPublisher, InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry,
        TriggerCompletionHandler triggerCompletionHandler, TriggerDispatcher triggerDispatcher,
        TriggerExecutionErrorEventListener triggerExecutionErrorEventListener,
        RemoteTriggerExecutionService triggerExecutionService, RemoteTriggerStateService triggerStateService,
        RemoteWorkflowService workflowService) {

        this.applicationEventListeners = applicationEventListeners;
        this.errorEventListeners = errorEventListeners;
        this.eventPublisher = eventPublisher;
        this.instanceWorkflowAccessorRegistry = instanceWorkflowAccessorRegistry;
        this.triggerCompletionHandler = triggerCompletionHandler;
        this.triggerDispatcher = triggerDispatcher;
        this.triggerExecutionErrorEventListener = triggerExecutionErrorEventListener;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    /**
     *
     * @param applicationEvent
     */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        for (ApplicationEventListener applicationEventListener : applicationEventListeners) {
            applicationEventListener.onApplicationEvent(applicationEvent);
        }
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        for (ErrorEventListener errorEventListener : errorEventListeners) {
            errorEventListener.onErrorEvent(errorEvent);
        }
    }

    public void onTriggerExecutionErrorEvent(TriggerExecutionErrorEvent triggerExecutionErrorEvent) {
        triggerExecutionErrorEventListener.onTriggerExecutionErrorEvent(triggerExecutionErrorEvent);
    }

    /**
     * Complete a trigger of a given workflow execution.
     *
     * @param triggerExecutionCompleteEvent The trigger execution event to complete.
     */
    // TODO @Transactional
    public void onTriggerExecutionCompleteEvent(TriggerExecutionCompleteEvent triggerExecutionCompleteEvent) {
        TriggerExecution triggerExecution = triggerExecutionCompleteEvent.getTriggerExecution();

        handleTriggerExecutionCompletion(triggerExecution);
    }

    // TODO @Transactional
    public void onTriggerListenerEvent(TriggerListenerEvent triggerListenerEvent) {
        TriggerListenerEvent.ListenerParameters listenerParameters = triggerListenerEvent.getListenerParameters();

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .output(listenerParameters.output())
            .workflowExecutionId(listenerParameters.workflowExecutionId())
            .build();

        handleTriggerExecutionCompletion(triggerExecution);
    }

    /**
     * Invoked every poll interval (5 mins by default) for a given workflow execution to dispatch request for trigger's
     * handler execution.
     *
     * @param triggerPollEvent The trigger poll event.
     */
    // TODO @Transactional
    public void onTriggerPollEvent(TriggerPollEvent triggerPollEvent) {
        WorkflowExecutionId workflowExecutionId = triggerPollEvent.getWorkflowExecutionId();

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
            .build();

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Handling poll trigger id={}, type='{}', name='{}', workflowExecutionId='{}' executed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName(),
                triggerExecution.getWorkflowExecutionId());
        }

        dispatch(triggerExecution);
    }

    /**
     *
     * @param triggerWebhookEvent
     */
    // TODO @Transactional
    public void onTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
        TriggerWebhookEvent.WebhookParameters webhookParameters = triggerWebhookEvent.getWebhookParameters();

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, webhookParameters.webhookRequest()))
            .workflowExecutionId(webhookParameters.workflowExecutionId())
            .workflowTrigger(getWorkflowTrigger(webhookParameters.workflowExecutionId()))
            .build();

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Dispatching webhook trigger id={}, type='{}', name='{}', workflowExecutionId='{}' executed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName(),
                triggerExecution.getWorkflowExecutionId());
        }

        dispatch(triggerExecution);
    }

    private void dispatch(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        InstanceWorkflowAccessor instanceWorkflowAccessor = instanceWorkflowAccessorRegistry
            .getInstanceWorkflowAccessor(workflowExecutionId.getInstanceType());

        triggerExecution = triggerExecutionService.create(
            triggerExecution.evaluate(
                instanceWorkflowAccessor.getInputMap(
                    workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId())));

        triggerExecution.setState(OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null));

        try {
            triggerDispatcher.dispatch(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            eventPublisher.publishEvent(new TriggerExecutionErrorEvent(triggerExecution));
        }
    }

    private void handleTriggerExecutionCompletion(TriggerExecution triggerExecution) {
        try {
            triggerCompletionHandler.handle(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            eventPublisher.publishEvent(new TriggerExecutionErrorEvent(triggerExecution));
        }
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getTriggerName()));
    }

}
