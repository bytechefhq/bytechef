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

package com.bytechef.platform.coordinator.job.exception;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class TaskExecutionErrorType extends AbstractErrorType {

    public static final TaskExecutionErrorType TASK_EXECUTION_FAILED = new TaskExecutionErrorType(100);

    private TaskExecutionErrorType(int errorKey) {
        super(TaskExecution.class, errorKey);
    }
}
