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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Job.Status;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class JobStatusApplicationEvent extends AbstractEvent implements ApplicationEvent {

    public static final String JOB_STATUS = "job.status";

    private long jobId;
    private Status status;
    private boolean suspended;

    private JobStatusApplicationEvent() {
    }

    public JobStatusApplicationEvent(long jobId, Status status) {
        super(TaskCoordinatorMessageRoute.APPLICATION_EVENTS);

        Assert.notNull(status, "'status' must not be null");

        this.jobId = jobId;
        this.status = status;
    }

    /**
     * The {@link Status#STOPPED} status of a job that stopped because a task suspended it, rather than because it was
     * stopped.
     */
    public static JobStatusApplicationEvent suspended(long jobId) {
        JobStatusApplicationEvent jobStatusApplicationEvent = new JobStatusApplicationEvent(jobId, Status.STOPPED);

        jobStatusApplicationEvent.suspended = true;

        return jobStatusApplicationEvent;
    }

    public long getJobId() {
        return jobId;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public String toString() {
        return "JobStatusApplicationEvent{" +
            "jobId=" + jobId +
            ", status=" + status +
            ", suspended=" + suspended +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
