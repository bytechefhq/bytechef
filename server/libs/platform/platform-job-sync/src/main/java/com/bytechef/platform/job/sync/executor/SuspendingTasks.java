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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import java.util.List;

/**
 * Tells the workflow steps that can pause a run: approval and wait steps.
 *
 * @author Ivica Cardic
 */
public final class SuspendingTasks {

    private static final List<String> SUSPENDING_TASK_TYPE_PREFIXES = List.of("approval/", "wait/", "waitForApproval/");

    private SuspendingTasks() {
    }

    public static boolean isSuspending(WorkflowTask workflowTask) {
        String type = workflowTask.getType();

        return type != null && SUSPENDING_TASK_TYPE_PREFIXES.stream()
            .anyMatch(type::startsWith);
    }
}
