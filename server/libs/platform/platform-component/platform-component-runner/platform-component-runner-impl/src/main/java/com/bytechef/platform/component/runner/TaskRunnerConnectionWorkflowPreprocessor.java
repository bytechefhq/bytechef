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

package com.bytechef.platform.component.runner;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;

import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.atlas.configuration.workflow.preprocessor.WorkflowDefinitionPreprocessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Keeps a task's {@code connections.taskRunner} entry in step with the runner it selected, so the editor offers the
 * runner's connection and the runner finds it at run time, whichever client saved the workflow.
 */
@Component
public class TaskRunnerConnectionWorkflowPreprocessor implements WorkflowDefinitionPreprocessor {

    static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    static final String COMPONENT_NAME = "componentName";
    static final String COMPONENT_VERSION = "componentVersion";
    static final String CONNECTIONS = "connections";
    static final String PARAMETERS = "parameters";

    private final TaskRunnerRegistry taskRunnerRegistry;

    public TaskRunnerConnectionWorkflowPreprocessor(@Lazy TaskRunnerRegistry taskRunnerRegistry) {
        this.taskRunnerRegistry = taskRunnerRegistry;
    }

    @Override
    public boolean process(Map<String, Object> workflowDefinition) {
        return visit(workflowDefinition);
    }

    @SuppressWarnings("unchecked")
    private boolean visit(Object node) {
        boolean changed = false;

        if (node instanceof Map<?, ?> map) {
            if (WorkflowTaskUtils.isWorkflowTaskMap(map)) {
                changed = processTask((Map<String, Object>) map);
            }

            for (Object value : map.values()) {
                if (visit(value)) {
                    changed = true;
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                if (visit(item)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    @SuppressWarnings("unchecked")
    private boolean processTask(Map<String, Object> task) {
        String runnerType = runnerType(task);

        if (runnerType == null) {
            return false;
        }

        TaskRunner taskRunner;

        try {
            taskRunner = taskRunnerRegistry.getTaskRunner(runnerType);
        } catch (TaskRunnerNotEnabledException exception) {
            return false;
        }

        Optional<TaskRunnerConnection> connection = taskRunner.getConnection();

        Map<String, Object> connections = task.get(CONNECTIONS) instanceof Map<?, ?> map
            ? (Map<String, Object>) map
            : null;

        if (connection.isEmpty()) {
            if (connections == null || !connections.containsKey(TASK_RUNNER)) {
                return false;
            }

            connections.remove(TASK_RUNNER);

            if (connections.isEmpty()) {
                task.remove(CONNECTIONS);
            }

            return true;
        }

        TaskRunnerConnection taskRunnerConnection = connection.get();

        Map<String, Object> entry = new LinkedHashMap<>();

        entry.put(COMPONENT_NAME, taskRunnerConnection.componentName());
        entry.put(COMPONENT_VERSION, taskRunnerConnection.componentVersion());
        entry.put(AUTHORIZATION_REQUIRED, true);

        if (connections == null) {
            connections = new LinkedHashMap<>();

            task.put(CONNECTIONS, connections);
        }

        if (Objects.equals(connections.get(TASK_RUNNER), entry)) {
            return false;
        }

        connections.put(TASK_RUNNER, entry);

        return true;
    }

    private static @Nullable String runnerType(Map<String, Object> task) {
        if (!(task.get(PARAMETERS) instanceof Map<?, ?> parameters)) {
            return null;
        }

        if (!(parameters.get(TASK_RUNNER) instanceof Map<?, ?> taskRunner)) {
            return null;
        }

        Object type = taskRunner.get(TYPE);

        if (!(type instanceof String string) || string.isBlank()) {
            return null;
        }

        return string;
    }
}
