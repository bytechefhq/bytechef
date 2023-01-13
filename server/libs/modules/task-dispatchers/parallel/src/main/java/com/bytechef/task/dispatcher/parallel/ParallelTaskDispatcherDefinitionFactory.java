
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

package com.bytechef.task.dispatcher.parallel;

import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.display;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.PARALLEL;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.TASKS;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ParallelTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = TaskDispatcherDSL.taskDispatcher(
        PARALLEL)
        .display(
            display("Parallel")
                .description(
                    "Run collection of tasks in parallel, without waiting until the previous function has completed."))
        .taskProperties(array(TASKS)
            .description("The task to use in each iteration.")
            .items(task()));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
