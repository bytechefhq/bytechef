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

package com.bytechef.task.dispatcher.terminate;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.terminate.constants.TerminateTaskDispatcherConstants.MESSAGE;
import static com.bytechef.task.dispatcher.terminate.constants.TerminateTaskDispatcherConstants.TERMINATE;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class TerminateTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TERMINATE_TASK_DISPATCHER_DEFINITION =
        taskDispatcher(TERMINATE)
            .title("Stop Job")
            .description("Stops a job execution with specified status and message.")
            .icon("path:assets/terminate.svg")
            .properties(
                string(MESSAGE)
                    .description("Reason for stopping the job."));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TERMINATE_TASK_DISPATCHER_DEFINITION;
    }
}
