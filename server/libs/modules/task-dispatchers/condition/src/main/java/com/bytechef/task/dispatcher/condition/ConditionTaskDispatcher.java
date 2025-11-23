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

package com.bytechef.task.dispatcher.condition;

import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITION;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.ErrorHandlingTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.condition.util.ConditionTaskUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class ConditionTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public ConditionTaskDispatcher(
        ContextService contextService, Evaluator evaluator, ApplicationEventPublisher eventPublisher,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        super(eventPublisher);

        this.contextService = contextService;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void doDispatch(TaskExecution taskExecution) {
        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        List<WorkflowTask> subWorkflowTasks;

        if (ConditionTaskUtils.resolveCase(taskExecution)) {
            subWorkflowTasks = getSubWorkflowTasks(taskExecution, CASE_TRUE);
        } else {
            subWorkflowTasks = getSubWorkflowTasks(taskExecution, CASE_FALSE);
        }

        if (!subWorkflowTasks.isEmpty()) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.getFirst();

            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(taskExecution.getJobId())
                .parentId(taskExecution.getId())
                .priority(taskExecution.getPriority())
                .taskNumber(1)
                .workflowTask(subWorkflowTask)
                .build();

            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(Validate.notNull(taskExecution.getId(), "id"),
                    Context.Classname.TASK_EXECUTION));

            subTaskExecution.evaluate(context, evaluator);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(
                Validate.notNull(subTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    Validate.notNull(subTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            taskExecution.setStartDate(Instant.now());
            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        }

    }

    private static List<WorkflowTask> getSubWorkflowTasks(TaskExecution conditionTaskExecution, String caseTrue) {
        return MapUtils
            .getList(
                conditionTaskExecution.getParameters(), caseTrue, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), CONDITION + "/v1")) {
            return this;
        }

        return null;
    }
}
