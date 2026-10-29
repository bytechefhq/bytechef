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
     * The job metadata key set while a job is stopped on a deprecated wait for approval task. It is removed when the
     * approval resumes the job.
     */
    public static final String WAIT_FOR_APPROVAL = "waitForApproval";

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
     * Tells whether the job was stopped for good, by a user, rather than suspended until it resumes or waiting on a
     * deprecated wait for approval task.
     */
    public static boolean isStoppedByUser(Job job) {
        return job.getStatus() == Job.Status.STOPPED && !isSuspended(job) &&
            !Boolean.TRUE.equals(job.getMetadata(WAIT_FOR_APPROVAL));
    }
}
