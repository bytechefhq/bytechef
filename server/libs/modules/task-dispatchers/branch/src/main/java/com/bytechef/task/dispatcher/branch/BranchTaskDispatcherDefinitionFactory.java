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

package com.bytechef.task.dispatcher.branch;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.WORKFLOW_ID;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.TASKS;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Component
public class BranchTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    public BranchTaskDispatcherDefinitionFactory(Optional<TaskListOutputDataSource> taskListOutputDataSource) {
        this.taskDispatcherDefinition = taskDispatcher(BRANCH)
            .title("Branch")
            .description("Executes one and only one branch of execution based on the `expression` value.")
            .icon("path:assets/branch.svg")
            .properties(
                string(EXPRESSION)
                    .label("Expression")
                    .description("Defines expression upon which evaluation the proper branch continues execution.")
                    .controlType(Property.ControlType.FORMULA_MODE))
            .output(inputParameters -> taskListOutputDataSource
                .map(dataSource -> output(inputParameters, dataSource))
                .orElse(null))
            .taskProperties(
                array(CASES)
                    .description(
                        "The list of tasks to execute if the result of expression matches the 'key' value.")
                    .items(
                        object()
                            .properties(
                                string(KEY),
                                array(TASKS)
                                    .description("The list of tasks.")
                                    .items(task()))),
                array(DEFAULT)
                    .description(
                        "The list of tasks to execute if the result of expression does not match any of 'key' values.")
                    .items(task()));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    protected static OutputResponse output(
        Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

        String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
        long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

        List<Map<String, ?>> cases = MapUtils.getList(
            inputParameters, CASES, new TypeReference<>() {}, List.of());

        List<Map<String, ?>> lastCaseTasks;

        if (cases.isEmpty()) {
            lastCaseTasks = MapUtils.getList(inputParameters, DEFAULT, new TypeReference<>() {}, List.of());
        } else {
            Map<String, ?> firstCase = cases.getFirst();

            if (!firstCase.containsKey(TASKS)) {
                return null;
            }

            lastCaseTasks = MapUtils.getList(firstCase, TASKS, new TypeReference<>() {}, List.of());
        }

        if (lastCaseTasks.isEmpty()) {
            return null;
        }

        Map<String, ?> lastTask = lastCaseTasks.getLast();

        String lastTaskName = MapUtils.getString(lastTask, "name");
        String lastTaskType = MapUtils.getString(lastTask, "type");

        if (lastTaskType == null) {
            return null;
        }

        OutputResponse lastTaskOutput = taskListOutputDataSource.getLastTaskOutput(
            workflowId, lastTaskName, lastTaskType, environmentId);

        if (lastTaskOutput == null) {
            return null;
        }

        ModifiableValueProperty<?, ?> lastTaskSchema = (ModifiableValueProperty<?, ?>) lastTaskOutput.getOutputSchema();

        Object lastTaskSampleOutput = lastTaskOutput.getSampleOutput();

        if (lastTaskSampleOutput != null) {
            return OutputResponse.of(lastTaskSchema, lastTaskSampleOutput);
        }

        return OutputResponse.of(lastTaskSchema);
    }
}
