
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

package com.bytechef.task.dispatcher.each;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.LIST;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link TaskDispatcher} implementation which implements a parallel for-each construct. The dispatcher works by
 * executing the <code>iteratee</code> function on each item on the <code>list</code>.
 *
 * @author Arik Cohen
 * @since Apr 25, 2017
 */
public class EachTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final MessageBroker messageBroker;
    private final ContextService contextService;
    private final CounterService counterService;

    public EachTaskDispatcher(
        TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService,
        MessageBroker messageBroker,
        ContextService contextService,
        CounterService counterService,
        TaskEvaluator taskEvaluator) {
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.messageBroker = messageBroker;
        this.contextService = contextService;
        this.counterService = counterService;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        Map<String, Object> iteratee = MapUtils.getRequiredMap(taskExecution.getParameters(), ITERATEE);
        List<Object> list = MapUtils.getRequiredList(taskExecution.getParameters(), LIST, Object.class);

        taskExecutionService.updateStatus(taskExecution.getId(), TaskStatus.STARTED, LocalDateTime.now(), null);

        if (list.isEmpty()) {
            taskExecution.setStartTime(LocalDateTime.now());
            taskExecution.setEndTime(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, taskExecution);
        } else {
            counterService.set(taskExecution.getId(), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                TaskExecution iterateeTaskExecution = TaskExecution.of(
                    taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), i + 1,
                    new WorkflowTask(iteratee));

                Context context = contextService.peek(taskExecution.getId());

                context.put(MapUtils.getString(taskExecution.getParameters(), ITEM_VAR, ITEM), item);
                context.put(MapUtils.getString(taskExecution.getParameters(), ITEM_INDEX, ITEM_INDEX), i);

                contextService.push(iterateeTaskExecution.getId(), context);

                iterateeTaskExecution.evaluate(taskEvaluator, context);

                iterateeTaskExecution = taskExecutionService.create(iterateeTaskExecution);

                taskDispatcher.dispatch(iterateeTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends TaskExecution> resolve(Task task) {
        if (Objects.equals(task.getType(), EachTaskDispatcherConstants.EACH + "/v" + VERSION_1)) {
            return this;
        }
        return null;
    }
}
