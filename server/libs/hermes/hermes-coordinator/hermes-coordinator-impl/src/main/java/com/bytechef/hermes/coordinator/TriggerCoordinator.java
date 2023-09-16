
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
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessor;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.execution.message.broker.ListenerParameters;
import com.bytechef.hermes.execution.message.broker.WebhookParameters;
import com.bytechef.hermes.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.RemoteTriggerExecutionService;
import com.bytechef.hermes.execution.service.RemoteTriggerStateService;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCoordinator.class);

    private final InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry;
    private final MessageBroker messageBroker;
    private final TriggerCompletionHandler triggerCompletionHandler;
    private final TriggerDispatcher triggerDispatcher;
    private final RemoteTriggerExecutionService triggerExecutionService;
    private final RemoteTriggerStateService triggerStateService;
    private final RemoteWorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCoordinator(
        InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry, MessageBroker messageBroker,
        TriggerCompletionHandler triggerCompletionHandler, TriggerDispatcher triggerDispatcher,
        RemoteTriggerExecutionService triggerExecutionService, RemoteTriggerStateService triggerStateService,
        RemoteWorkflowService workflowService) {

        this.instanceWorkflowAccessorRegistry = instanceWorkflowAccessorRegistry;
        this.messageBroker = messageBroker;
        this.triggerCompletionHandler = triggerCompletionHandler;
        this.triggerDispatcher = triggerDispatcher;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    /**
     * Complete a trigger of a given workflow execution.
     *
     * @param triggerExecution The trigger to complete.
     */
    public void handleTriggersComplete(TriggerExecution triggerExecution) {
        try {
            triggerCompletionHandler.handle(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            messageBroker.send(SystemMessageRoute.ERRORS, triggerExecution);
        }
    }

    public void handleListeners(ListenerParameters listenerParameters) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .output(listenerParameters.output())
            .workflowExecutionId(listenerParameters.workflowExecutionId())
            .build();

        handleTriggersComplete(triggerExecution);
    }

    /**
     * Invoked every poll interval (5 mins by default) for a given workflow execution to dispatch request for trigger's
     * handler execution.
     *
     * @param workflowExecutionId The workflowExecutionId.
     */
    public void handlePolls(WorkflowExecutionId workflowExecutionId) {
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
     * @param webhookParameters
     */
    public void handleWebhooks(WebhookParameters webhookParameters) {
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

            messageBroker.send(SystemMessageRoute.ERRORS, triggerExecution);
        }
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getTriggerName()));
    }

}
