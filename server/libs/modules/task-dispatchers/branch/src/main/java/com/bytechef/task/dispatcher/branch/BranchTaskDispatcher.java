/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.task.dispatcher.branch;

import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.ErrorHandlingTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.type.TypeReference;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 * @see BranchTaskCompletionHandler
 */
public class BranchTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public BranchTaskDispatcher(
        ContextService contextService, Evaluator evaluator, ApplicationEventPublisher eventPublisher,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        super(eventPublisher);

        this.evaluator = evaluator;
        this.contextService = contextService;
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

        Map<String, ?> selectedCase = resolveCase(taskExecution);

        if (selectedCase.containsKey(TASKS)) {
            List<WorkflowTask> subWorkflowTasks = MapUtils
                .getList(
                    selectedCase, TASKS, new TypeReference<Map<String, ?>>() {}, List.of())
                .stream()
                .map(WorkflowTask::new)
                .toList();

            if (subWorkflowTasks.isEmpty()) {
                taskExecution.setStartDate(Instant.now());
                taskExecution.setEndDate(Instant.now());
                taskExecution.setExecutionTime(0);

                eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
            } else {
                WorkflowTask subWorkflowTask = subWorkflowTasks.get(0);

                TaskExecution subTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(1)
                    .workflowTask(subWorkflowTask)
                    .build();

                Map<String, ?> context = taskFileStorage.readContextValue(
                    contextService.peek(Validate.notNull(taskExecution.getId(), "id"), Classname.TASK_EXECUTION));

                subTaskExecution.evaluate(context, evaluator);

                subTaskExecution = taskExecutionService.create(subTaskExecution);

                contextService.push(
                    Validate.notNull(subTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
                    taskFileStorage.storeContextValue(
                        Validate.notNull(subTaskExecution.getId(), "id"), Classname.TASK_EXECUTION, context));

                taskDispatcher.dispatch(subTaskExecution);
            }
        } else {
            taskExecution.setStartDate(Instant.now());
            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(0);
            // TODO check, it seems wrong

            if (selectedCase.get("value") != null) {
                taskExecution.setOutput(
                    taskFileStorage.storeTaskExecutionOutput(
                        Validate.notNull(taskExecution.getId(), "id"), selectedCase.get("value")));
            }

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        }

    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), BRANCH + "/v1")) {
            return this;
        }

        return null;
    }

    private Map<String, ?> resolveCase(TaskExecution taskExecution) {
        Object expression = MapUtils.getRequired(taskExecution.getParameters(), EXPRESSION);
        List<Map<String, Object>> cases = MapUtils.getRequiredList(
            taskExecution.getParameters(), CASES, new TypeReference<>() {});

        for (Map<String, Object> oneCase : cases) {
            Object key = MapUtils.getRequired(oneCase, KEY);

            if (key.equals(expression)) {
                return oneCase;
            }
        }

        return Map.of(TASKS, MapUtils.get(taskExecution.getParameters(), DEFAULT, new TypeReference<>() {}, List.of()));
    }
}
