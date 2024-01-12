/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.task.dispatcher.fork.join;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ForkJoinTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION =
        taskDispatcher(ForkJoinTaskDispatcherConstants.FORK_JOIN)
            .title("Fork/Join")
            .description(
                "Executes each branch in parallel (list of tasks) as a separate and isolated sub-flow. Branches are executed internally in sequence.")
            .icon("path:assets/fork-join.svg")
            .taskProperties(array(ForkJoinTaskDispatcherConstants.BRANCHES)
                .description("The list of sequences of tasks to execute in parallel.")
                .items(array().description("The list of tasks that executes sequentially.")
                    .items(task())));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
