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

import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class TaskRunnerConnectionUtils {

    private TaskRunnerConnectionUtils() {
    }

    public static Optional<TaskRunnerConnection> fetchConnection(
        Map<String, ?> parameters, TaskRunnerRegistry taskRunnerRegistry) {

        if (parameters == null || !(parameters.get(TASK_RUNNER) instanceof Map<?, ?> taskRunnerParameters)) {
            return Optional.empty();
        }

        if (!(taskRunnerParameters.get(TYPE) instanceof String type) || type.isBlank()) {
            return Optional.empty();
        }

        TaskRunner taskRunner;

        try {
            taskRunner = taskRunnerRegistry.getTaskRunner(type);
        } catch (TaskRunnerNotEnabledException exception) {
            return Optional.empty();
        }

        return taskRunner.getConnection();
    }
}
