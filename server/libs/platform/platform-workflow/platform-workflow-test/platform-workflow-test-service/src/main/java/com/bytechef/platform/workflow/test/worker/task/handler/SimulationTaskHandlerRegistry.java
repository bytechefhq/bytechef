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

package com.bytechef.platform.workflow.test.worker.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class SimulationTaskHandlerRegistry implements TaskHandlerRegistry {

    private final TaskHandlerRegistry taskHandlerRegistry;

    @SuppressFBWarnings("EI")
    public SimulationTaskHandlerRegistry(TaskHandlerRegistry taskHandlerRegistry) {
        this.taskHandlerRegistry = taskHandlerRegistry;
    }

    @Override
    public TaskHandler<?> getTaskHandler(String type) {
        TaskHandler<?> taskHandler = taskHandlerRegistry.getTaskHandler(type);

        if (!(taskHandler instanceof AbstractTaskHandler)) {
            return taskExecution -> {
                throw new TaskExecutionException("Task type '%s' cannot be simulated".formatted(type));
            };
        }

        return taskExecution -> handle(type, taskHandler, taskExecution);
    }

    private static Object handle(String type, TaskHandler<?> taskHandler, TaskExecution taskExecution)
        throws TaskExecutionException {

        if (!MapUtils.getBoolean(taskExecution.getMetadata(), MetadataConstants.DRY_RUN, false)) {
            throw new TaskExecutionException(
                "Task type '%s' reached the simulation worker without the dry-run flag".formatted(type));
        }

        return taskHandler.handle(taskExecution);
    }
}
