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

package com.bytechef.task.dispatcher.on.error;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.on.error.constants.OnErrorTaskDispatcherConstants.MAIN_BRANCH;
import static com.bytechef.task.dispatcher.on.error.constants.OnErrorTaskDispatcherConstants.ON_ERROR;
import static com.bytechef.task.dispatcher.on.error.constants.OnErrorTaskDispatcherConstants.ON_ERROR_BRANCH;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class OnErrorTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition ON_ERROR_TASK_DISPATCHER_DEFINITION =
        taskDispatcher(ON_ERROR)
            .title("Error Handler")
            .description("Triggers an error branch with an error object if an exception occurs in the main branch.")
            .icon("path:assets/onError.svg")
            .taskProperties(
                array(MAIN_BRANCH)
                    .description(
                        "The list of tasks to execute that will trigger on error branch in case of exception.")
                    .items(task()),
                array(ON_ERROR_BRANCH)
                    .description(
                        "The list of tasks to execute when exception occurs in the main branch.")
                    .items(task()));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return ON_ERROR_TASK_DISPATCHER_DEFINITION;
    }
}
