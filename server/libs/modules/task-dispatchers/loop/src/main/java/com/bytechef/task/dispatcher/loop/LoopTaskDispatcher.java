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

package com.bytechef.task.dispatcher.loop;

import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEMS;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_FOREVER;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A {@link TaskDispatcher} implementation which implements a loop construct. The dispatcher works by executing the
 * <code>iteratee</code> function on each item on the <code>stream</code>.
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class LoopTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public LoopTaskDispatcher(
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
        boolean loopForever = MapUtils.getBoolean(taskExecution.getParameters(), LOOP_FOREVER, false);
        List<WorkflowTask> iterateeWorkflowTasks = MapUtils.getRequiredList(
            taskExecution.getParameters(), ITERATEE, WorkflowTask.class);
        List<?> items = MapUtils.getList(taskExecution.getParameters(), ITEMS, Collections.emptyList());

        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (loopForever || !items.isEmpty()) {
            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(taskExecution.getJobId())
                .parentId(taskExecution.getId())
                .priority(taskExecution.getPriority())
                .taskNumber(0)
                .workflowTask(iterateeWorkflowTasks.getFirst())
                .build();

            Map<String, Object> newContext = new HashMap<>(
                taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)));

            Map<String, Object> workflowTaskNameMap = new HashMap<>();

            if (!items.isEmpty()) {
                workflowTaskNameMap.put(ITEM, items.getFirst());
            }

            workflowTaskNameMap.put(INDEX, 0);

            WorkflowTask loopWorkflowTask = taskExecution.getWorkflowTask();

            newContext.put(loopWorkflowTask.getName(), workflowTaskNameMap);

            subTaskExecution = taskExecutionService.create(subTaskExecution.evaluate(newContext, evaluator));

            contextService.push(
                Validate.notNull(subTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    Validate.notNull(subTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                    newContext));

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            taskExecution.setStartDate(Instant.now());
            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        }

    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), LOOP + "/v1")) {
            return this;
        }

        return null;
    }
}
