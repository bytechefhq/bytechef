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
 * @author Ivica Cardic
 */
public final class SuspendedJobs {

    public static final String JOB_RESUME_ID = "jobResumeId";

    public static final String WAIT_FOR_APPROVAL = "waitForApproval";

    private SuspendedJobs() {
    }

    public static boolean isSuspended(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(JOB_RESUME_ID) != null;
    }

    public static boolean isStoppedByUser(Job job) {
        return job.getStatus() == Job.Status.STOPPED && !isSuspended(job) &&
            !Boolean.TRUE.equals(job.getMetadata(WAIT_FOR_APPROVAL));
    }
}
