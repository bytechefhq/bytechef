
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.task.dispatcher.branch;

import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.MapUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 * @see BranchTaskCompletionHandler
 */
public class BranchTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ApplicationEventPublisher eventPublisher;
    private final RemoteContextService contextService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final RemoteTaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public BranchTaskDispatcher(
        ApplicationEventPublisher eventPublisher, RemoteContextService contextService,
        TaskDispatcher<? super Task> taskDispatcher, RemoteTaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        Map<String, ?> selectedCase = resolveCase(taskExecution);

        if (selectedCase.containsKey(TASKS)) {
            List<WorkflowTask> subWorkflowTasks = MapUtils.getList(
                selectedCase, TASKS, WorkflowTask.class, Collections.emptyList());

            if (subWorkflowTasks.isEmpty()) {
                taskExecution.setStartDate(LocalDateTime.now());
                taskExecution.setEndDate(LocalDateTime.now());
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

                Map<String, ?> context = workflowFileStorageFacade.readContextValue(
                    contextService.peek(Objects.requireNonNull(taskExecution.getId()), Classname.TASK_EXECUTION));

                subTaskExecution.evaluate(context);

                subTaskExecution = taskExecutionService.create(subTaskExecution);

                contextService.push(
                    Objects.requireNonNull(subTaskExecution.getId()), Classname.TASK_EXECUTION,
                    workflowFileStorageFacade.storeContextValue(
                        subTaskExecution.getId(), Classname.TASK_EXECUTION, context));

                taskDispatcher.dispatch(subTaskExecution);
            }
        } else {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);
            // TODO check, it seems wrong

            if (selectedCase.get("value") != null) {
                taskExecution.setOutput(
                    workflowFileStorageFacade.storeTaskExecutionOutput(
                        Objects.requireNonNull(taskExecution.getId()), selectedCase.get("value")));
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
        List<Map<String, Object>> cases = MapUtils.getList(
            taskExecution.getParameters(), CASES, new ParameterizedTypeReference<>() {});

        Assert.notNull(cases, "you must specify 'cases' in a branch statement");

        for (Map<String, Object> oneCase : cases) {
            Object key = MapUtils.getRequired(oneCase, KEY);

            if (key.equals(expression)) {
                return oneCase;
            }
        }

        return MapUtils.getMap(taskExecution.getParameters(), DEFAULT, Collections.emptyMap());
    }
}
