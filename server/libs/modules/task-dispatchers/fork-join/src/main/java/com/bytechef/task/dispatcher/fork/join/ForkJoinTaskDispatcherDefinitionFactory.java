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

package com.bytechef.task.dispatcher.fork.join;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.WORKFLOW_ID;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Component
public class ForkJoinTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    public ForkJoinTaskDispatcherDefinitionFactory(Optional<TaskListOutputDataSource> taskListOutputDataSource) {
        this.taskDispatcherDefinition = taskDispatcher(ForkJoinTaskDispatcherConstants.FORK_JOIN)
            .title("Fork/Join")
            .description(
                "Executes each branch in parallel (list of tasks) as a separate and isolated sub-flow. Branches are executed internally in sequence.")
            .icon("path:assets/fork-join.svg")
            .output(inputParameters -> taskListOutputDataSource
                .map(dataSource -> output(inputParameters, dataSource))
                .orElse(null))
            .taskProperties(array(ForkJoinTaskDispatcherConstants.BRANCHES)
                .description("The list of sequences of tasks to execute in parallel.")
                .items(array().description("The list of tasks that executes sequentially.")
                    .items(task())));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    protected static OutputResponse output(
        Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

        String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
        long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

        List<List<Map<String, ?>>> branches = MapUtils.getList(
            inputParameters, ForkJoinTaskDispatcherConstants.BRANCHES, new TypeReference<>() {}, List.of());

        if (branches.isEmpty()) {
            return null;
        }

        List<ModifiableValueProperty<?, ?>> branchProperties = new ArrayList<>();

        for (int i = 0; i < branches.size(); i++) {
            String branchPropertyName = ForkJoinTaskDispatcherConstants.BRANCH_OUTPUT_KEY_PREFIX + i;

            branchProperties.add(
                resolveBranchSchema(
                    branches.get(i), branchPropertyName, workflowId, environmentId, taskListOutputDataSource));
        }

        return OutputResponse.of(object().properties(branchProperties));
    }

    private static ModifiableValueProperty<?, ?> resolveBranchSchema(
        List<Map<String, ?>> branchTasks, String branchPropertyName, String workflowId, long environmentId,
        TaskListOutputDataSource taskListOutputDataSource) {

        if (branchTasks.isEmpty()) {
            return object(branchPropertyName);
        }

        Map<String, ?> lastTask = branchTasks.getLast();

        String lastTaskName = MapUtils.getString(lastTask, "name");
        String lastTaskType = MapUtils.getString(lastTask, "type");

        if (lastTaskType == null) {
            return object(branchPropertyName);
        }

        OutputResponse lastTaskOutput = taskListOutputDataSource.getLastTaskOutput(
            workflowId, lastTaskName, lastTaskType, environmentId);

        if (lastTaskOutput == null || lastTaskOutput.getOutputSchema() == null) {
            return object(branchPropertyName);
        }

        ModifiableValueProperty<?, ?> lastTaskSchema = (ModifiableValueProperty<?, ?>) lastTaskOutput.getOutputSchema();

        return lastTaskSchema.setName(branchPropertyName);
    }
}
