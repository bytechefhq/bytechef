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

package com.bytechef.atlas.file.storage;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class TaskExecutionLockedUpdatesImpl implements TaskExecutionLockedUpdates {

    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public TaskExecutionLockedUpdatesImpl(TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public TaskExecution putOutputEntry(long taskExecutionId, String key, @Nullable Object value) {
        return taskExecutionService.update(
            taskExecutionId, lockedTaskExecution -> putOutputEntry(lockedTaskExecution, key, value));
    }

    @Override
    public TaskExecution setEndDate(long taskExecutionId, Instant endDate) {
        return taskExecutionService.update(
            taskExecutionId, lockedTaskExecution -> lockedTaskExecution.setEndDate(endDate));
    }

    private void putOutputEntry(TaskExecution taskExecution, String key, @Nullable Object value) {
        long jobId = Objects.requireNonNull(taskExecution.getJobId());
        long taskExecutionId = Objects.requireNonNull(taskExecution.getId());

        Map<String, @Nullable Object> outputs;

        if (taskExecution.getOutput() == null) {
            outputs = new HashMap<>();
        } else {
            outputs = new HashMap<>(readOutputs(taskExecution.getOutput()));
        }

        outputs.put(key, value);

        taskExecution.setOutput(taskFileStorage.storeTaskExecutionOutput(jobId, taskExecutionId, outputs));
    }

    @SuppressWarnings("unchecked")
    private Map<String, @Nullable Object> readOutputs(FileEntry fileEntry) {
        return (Map<String, @Nullable Object>) taskFileStorage.readTaskExecutionOutput(fileEntry);
    }
}
