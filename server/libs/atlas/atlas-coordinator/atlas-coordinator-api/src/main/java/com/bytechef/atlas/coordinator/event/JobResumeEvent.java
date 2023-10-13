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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;

/**
 * @author Ivica Cardic
 */
public class JobResumeEvent extends AbstractEvent {

    private long jobId;

    private JobResumeEvent() {
    }

    public JobResumeEvent(long jobId) {
        super(CoordinatorMessageRoute.JOB_RESUME_EVENTS);

        this.jobId = jobId;
    }

    public long getJobId() {
        return jobId;
    }

    @Override
    public String toString() {
        return "JobResumeEvent{" +
            "jobId=" + jobId +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
