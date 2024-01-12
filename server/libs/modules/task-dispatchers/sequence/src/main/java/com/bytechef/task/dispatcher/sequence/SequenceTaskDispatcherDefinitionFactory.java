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

package com.bytechef.task.dispatcher.sequence;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher.TASKS;
import static com.bytechef.task.dispatcher.sequence.constant.SequenceTaskDispatcherConstants.SEQUENCE;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class SequenceTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(SEQUENCE)
        .title("Sequence")
        .description("Executes list of tasks in a sequence.")
        .icon("path:assets/sequence.svg")
        .taskProperties(array(TASKS)
            .description("The task to use in each iteration.")
            .items(task()));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
