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

package com.bytechef.atlas.execution.domain;

import org.jspecify.annotations.Nullable;

/**
 * Tells a job that is stopped on a suspend it will resume from apart from a job that was stopped for good.
 *
 * @author Ivica Cardic
 */
public final class SuspendedJobs {

    /**
     * The job metadata key holding the id a suspended job resumes with. It is removed when the job resumes.
     */
    public static final String JOB_RESUME_ID = "jobResumeId";

    /**
     * The job metadata key holding the id of the task execution that suspended the job.
     */
    public static final String TASK_EXECUTION_RESUME_ID = "taskExecutionResumeId";

    private SuspendedJobs() {
    }

    /**
     * Tells whether the job is stopped on a suspend, for example an approval it waits for, rather than stopped by a
     * user.
     */
    public static boolean isSuspended(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(JOB_RESUME_ID) != null;
    }

    /**
     * Tells whether the job's tasks may run: the job is running, or it is suspended on one branch while its other
     * branches keep going.
     */
    public static boolean canRunTasks(Job job) {
        return job.getStatus() == Job.Status.STARTED || isSuspended(job);
    }

    /**
     * The id of the task execution the job's pending suspend resumes, or null when the job waits on no suspend.
     */
    public static @Nullable Long getSuspendedTaskExecutionId(Job job) {
        if (job.getMetadata(JOB_RESUME_ID) == null) {
            return null;
        }

        return job.getMetadata(TASK_EXECUTION_RESUME_ID) instanceof Number number ? number.longValue() : null;
    }
}
