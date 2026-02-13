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

package com.bytechef.platform.webhook.event;

import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class SseStreamEvent implements MessageEvent<SseStreamMessageRoute> {

    public static final String EVENT_TYPE_COMPLETE = "complete";
    public static final String EVENT_TYPE_DATA = "data";
    public static final String EVENT_TYPE_ERROR = "error";
    public static final String EVENT_TYPE_JOB_STATUS = "job_status";
    public static final String EVENT_TYPE_TASK_STARTED = "task_started";

    private Instant createDate;
    private String eventType;
    private long jobId;
    private Map<String, Object> metadata = new HashMap<>();
    private @Nullable Object payload;
    private SseStreamMessageRoute route;

    private SseStreamEvent() {
    }

    public SseStreamEvent(long jobId, String eventType, @Nullable Object payload) {
        this.createDate = Instant.now();
        this.eventType = eventType;
        this.jobId = jobId;
        this.payload = payload;
        this.route = SseStreamMessageRoute.SSE_STREAM_EVENTS;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    public String getEventType() {
        return eventType;
    }

    public long getJobId() {
        return jobId;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public Object getMetadata(String name) {
        return metadata.get(name);
    }

    public @Nullable Object getPayload() {
        return payload;
    }

    @Override
    public SseStreamMessageRoute getRoute() {
        return route;
    }

    @Override
    public void putMetadata(String name, Object value) {
        metadata.put(name, value);
    }

    @Override
    public String toString() {
        return "SseStreamEvent{" +
            "eventType='" + eventType + '\'' +
            ", jobId=" + jobId +
            ", createDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
