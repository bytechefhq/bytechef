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

package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class JobSuspends {

    private JobSuspends() {
    }

    /**
     * Tells whether a completed task execution asked to suspend its job.
     */
    public static boolean isSuspending(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        return metadata.containsKey(MetadataConstants.JOB_RESUME_ID);
    }

    /**
     * Tells whether a job is suspended because an agent tool asked for a sub-workflow run and has not restarted yet,
     * including while a claimed resume is still on its way. The sub-workflow bridge resumes such a job, so callers
     * waiting for its result keep waiting instead of treating the suspend as the end of the run.
     */
    public static boolean isAwaitingSubflow(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null &&
            isSuspendedForSubflow(job);
    }

    /**
     * Tells whether the job's latest suspend was requested by an agent tool to run a sub-workflow, whatever the job's
     * status is now, so a suspend event handled after the job already resumed is still recognized.
     */
    public static boolean isSuspendedForSubflow(Job job) {
        return Boolean.TRUE.equals(job.getMetadata(MetadataConstants.SUBFLOW_RESUME));
    }
}
