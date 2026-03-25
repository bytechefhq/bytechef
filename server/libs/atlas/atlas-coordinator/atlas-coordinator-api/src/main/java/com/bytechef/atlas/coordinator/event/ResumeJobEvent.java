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
import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ResumeJobEvent extends AbstractEvent {

    private long jobId;
    private @Nullable Map<String, ?> data;

    private ResumeJobEvent() {
    }

    public ResumeJobEvent(long jobId) {
        this(jobId, null);
    }

    public ResumeJobEvent(long jobId, @Nullable Map<String, ?> data) {
        super(TaskCoordinatorMessageRoute.JOB_RESUME_EVENTS);

        this.jobId = jobId;
        this.data = data == null ? null : Collections.unmodifiableMap(data);
    }

    public long getJobId() {
        return jobId;
    }

    public @Nullable Map<String, ?> getData() {
        return data == null ? null : Collections.unmodifiableMap(data);
    }

    @Override
    public String toString() {
        return "ResumeJobEvent{" +
            "jobId=" + jobId +
            ", data=" + data +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
