
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

package com.bytechef.task.dispatcher.subflow;

import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.string;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.task.dispatcher.subflow.constant.SubflowTaskDispatcherConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class SubflowTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(
        SubflowTaskDispatcherConstants.SUBFLOW)
            .title("Subflow")
            .description(
                "Starts a new job as a sub-flow of the current job. Output of the sub-flow job is the output of the task.")
            .icon("path:assets/subflow.svg")
            .properties(string(WorkflowConstants.WORKFLOW_ID)
                .label("Workflow Id")
                .description("The id of sub-workflow to execute."));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
