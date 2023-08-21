
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

package com.bytechef.atlas.execution.event;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.event.AbstractEvent;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class JobStatusEvent extends AbstractEvent {

    public static final String JOB_STATUS = "job.status";

    private long jobId;
    private Status status;

    private JobStatusEvent() {
    }

    public JobStatusEvent(long jobId, Status status) {
        super(JOB_STATUS);

        Assert.notNull(status, "'status' must not be null");

        this.jobId = jobId;
        this.status = status;
    }

    public long getJobId() {
        return jobId;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "JobStatusWorkflowEvent{" +
            "type='" + type + '\'' +
            ", jobId=" + jobId +
            ", status=" + status +
            ", createdDate=" + createdDate +
            "} ";
    }
}
