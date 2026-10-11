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

package com.bytechef.component.workflow.subflow.sync;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Executes a sub-workflow synchronously and returns its output.
 *
 * @author Ivica Cardic
 */
public interface SubflowSyncExecutor {

    /**
     * Runs the sub-workflow one nesting level below the job identified by {@code callerJobId}, or at the first level
     * when there is no calling job, and fails when that level exceeds the maximum sub-workflow depth.
     */
    @Nullable
    Object execute(
        String workflowUuid, String triggerName, Map<String, ?> inputs, boolean editorEnvironment,
        @Nullable Long callerJobId);

    /**
     * Returns true when the job was started by a synchronous job executor, which awaits it in-process, so it must not
     * suspend.
     */
    boolean isSyncJob(long jobId);
}
