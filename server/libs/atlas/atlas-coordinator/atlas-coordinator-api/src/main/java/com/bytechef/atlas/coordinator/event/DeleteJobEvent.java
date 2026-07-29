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

import java.util.Collections;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DeleteJobEvent {

    private final long jobId;
    private final Map<String, ?> metadata;

    public DeleteJobEvent(long jobId, Map<String, ?> metadata) {
        this.jobId = jobId;
        this.metadata = Collections.unmodifiableMap(metadata);
    }

    public long getJobId() {
        return jobId;
    }

    public Map<String, ?> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "DeleteJobEvent{" +
            "jobId=" + jobId +
            ", metadata=" + metadata +
            "} ";
    }
}
