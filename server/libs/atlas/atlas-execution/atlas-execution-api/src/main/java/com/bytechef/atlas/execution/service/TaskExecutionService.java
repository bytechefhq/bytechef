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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface TaskExecutionService {

    TaskExecution create(TaskExecution taskExecution);

    void delete(long id);

    void deleteJobTaskExecutions(long jobId);

    Optional<TaskExecution> fetchLastJobTaskExecution(long jobId);

    TaskExecution getTaskExecution(long id);

    TaskExecution getTaskExecutionForUpdate(long id);

    List<TaskExecution> getJobTaskExecutions(long jobId);

    List<TaskExecution> getParentTaskExecutions(long parentId);

    TaskExecution update(TaskExecution taskExecution);
}
