
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.ExceptionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.coordinator.job.InstanceFacade;
import com.bytechef.hermes.coordinator.job.InstanceFacadeRegistry;
import com.bytechef.hermes.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.hermes.workflow.domain.TriggerExecution;
import com.bytechef.hermes.workflow.service.TriggerExecutionService;
import com.bytechef.hermes.workflow.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCoordinator.class);

    private final InstanceFacadeRegistry instanceFacadeRegistry;
    private final MessageBroker messageBroker;
    private final TriggerCompletionHandler triggerCompletionHandler;
    private final TriggerDispatcher triggerDispatcher;
    private final TriggerExecutionService triggerExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCoordinator(
        InstanceFacadeRegistry instanceFacadeRegistry, MessageBroker messageBroker,
        TriggerCompletionHandler triggerCompletionHandler, TriggerDispatcher triggerDispatcher,
        TriggerExecutionService triggerExecutionService, WorkflowService workflowService) {

        this.instanceFacadeRegistry = instanceFacadeRegistry;
        this.messageBroker = messageBroker;
        this.triggerCompletionHandler = triggerCompletionHandler;
        this.triggerDispatcher = triggerDispatcher;
        this.triggerExecutionService = triggerExecutionService;
        this.workflowService = workflowService;
    }

    /**
     * Complete a trigger of a given workflow execution.
     *
     * @param triggerExecution The trigger to complete.
     */
    public void complete(TriggerExecution triggerExecution) {
        try {
            triggerCompletionHandler.handle(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            messageBroker.send(SystemMessageRoute.ERRORS, triggerExecution);
        }
    }

    /**
     * Invoked every poll interval (5 mins by default) for a given workflow execution to dispatch request for trigger's
     * handler execution.
     *
     * @param workflowExecutionId The workflowExecutionId.
     */
    public void poll(WorkflowExecutionId workflowExecutionId) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
            .build();

        InstanceFacade instanceFacade = instanceFacadeRegistry.getInstanceFacade(
            workflowExecutionId.getInstanceType());

        triggerExecution.evaluate(
            instanceFacade.getInputs(workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId()));

        triggerExecutionService.create(triggerExecution);

        try {
            triggerDispatcher.dispatch(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            messageBroker.send(SystemMessageRoute.ERRORS, triggerExecution);
        }

        logger.debug(
            "Poll interval for trigger id={}, type='{}', name='{}' executed",
            triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName());
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getWorkflowTriggerName()));
    }
}
