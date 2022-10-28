/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.event;

import com.bytechef.atlas.job.JobStatus;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class JobStatusWorkflowEvent extends WorkflowEvent {

    public static final String JOB_STATUS = "job.status";

    private String jobId;
    private JobStatus jobStatus;

    public JobStatusWorkflowEvent() {
        this.type = JOB_STATUS;
    }

    public JobStatusWorkflowEvent(String jobId, JobStatus jobStatus) {
        Assert.notNull(jobId, "jobId must not be null");
        Assert.notNull(jobStatus, "status must not be null");

        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.type = JOB_STATUS;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    @Override
    public String toString() {
        return "JobStatusWorkflowEvent{" + "jobId='"
                + jobId + '\'' + ", jobStatus="
                + jobStatus + "} "
                + super.toString();
    }
}
