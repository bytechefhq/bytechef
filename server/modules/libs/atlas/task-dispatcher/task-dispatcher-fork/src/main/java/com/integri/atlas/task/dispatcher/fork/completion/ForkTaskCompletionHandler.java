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

package com.integri.atlas.task.dispatcher.fork.completion;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.counter.repository.CounterRepository;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Handles {@link TaskExecution} completions which are the child
 * execution tasks of a parent <code>fork</code> {@link TaskExecution}.
 *
 * This handler will either execute the next task in the branch, or if
 * arrived at the last task, it will complete the branch and check if
 * all branches are now complete. If so, it will complete the overall
 * <code>fork</code> task.
 *
 * @author Arik Cohen
 * @since May 11, 2017
 */
public class ForkTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepo;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterRepository counterRepository;
    private final TaskDispatcher taskDispatcher;
    private final ContextRepository contextRepository;
    private final TaskEvaluator taskEvaluator;

    public ForkTaskCompletionHandler(
        TaskExecutionRepository aTaskExecutionRepo,
        TaskCompletionHandler aTaskCompletionHandler,
        CounterRepository aCounterRepository,
        TaskDispatcher aTaskDispatcher,
        ContextRepository aContextRepository,
        TaskEvaluator aTaskEvaluator
    ) {
        taskExecutionRepo = aTaskExecutionRepo;
        taskCompletionHandler = aTaskCompletionHandler;
        counterRepository = aCounterRepository;
        taskDispatcher = aTaskDispatcher;
        contextRepository = aContextRepository;
        taskEvaluator = aTaskEvaluator;
    }

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTaskExecution);
        mtask.setStatus(TaskStatus.COMPLETED);
        taskExecutionRepo.merge(mtask);

        if (aTaskExecution.getOutput() != null && aTaskExecution.getName() != null) {
            Context context = contextRepository.peek(
                aTaskExecution.getParentId() + "/" + aTaskExecution.getInteger("branch")
            );
            MapContext newContext = new MapContext(context.asMap());
            newContext.put(aTaskExecution.getName(), aTaskExecution.getOutput());
            contextRepository.push(
                aTaskExecution.getParentId() + "/" + aTaskExecution.getInteger("branch"),
                newContext
            );
        }

        TaskExecution fork = taskExecutionRepo.findOne(aTaskExecution.getParentId());
        List<List> list = fork.getList("branches", List.class);
        List<Map<String, Object>> branch = list.get(aTaskExecution.getInteger("branch"));
        if (aTaskExecution.getTaskNumber() < branch.size()) {
            Map<String, Object> task = branch.get(aTaskExecution.getTaskNumber());
            SimpleTaskExecution execution = SimpleTaskExecution.of(task);
            execution.setId(UUIDGenerator.generate());
            execution.setStatus(TaskStatus.CREATED);
            execution.setCreateTime(new Date());
            execution.set("branch", aTaskExecution.getInteger("branch"));
            execution.setTaskNumber(aTaskExecution.getTaskNumber() + 1);
            execution.setJobId(aTaskExecution.getJobId());
            execution.setParentId(aTaskExecution.getParentId());
            execution.setPriority(aTaskExecution.getPriority());
            Context context = contextRepository.peek(
                aTaskExecution.getParentId() + "/" + aTaskExecution.getInteger("branch")
            );
            contextRepository.push(execution.getId(), context);
            TaskExecution evaluatedExecution = taskEvaluator.evaluate(execution, context);
            taskExecutionRepo.create(evaluatedExecution);
            taskDispatcher.dispatch(evaluatedExecution);
        } else {
            long branchesLeft = counterRepository.decrement(aTaskExecution.getParentId());
            if (branchesLeft == 0) {
                SimpleTaskExecution forkTask = SimpleTaskExecution.of(
                    taskExecutionRepo.findOne(aTaskExecution.getParentId())
                );
                forkTask.setEndTime(new Date());
                forkTask.setExecutionTime(forkTask.getEndTime().getTime() - forkTask.getStartTime().getTime());
                taskCompletionHandler.handle(forkTask);
            }
        }
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        return aTaskExecution.getParentId() != null && aTaskExecution.get("branch") != null;
    }
}
