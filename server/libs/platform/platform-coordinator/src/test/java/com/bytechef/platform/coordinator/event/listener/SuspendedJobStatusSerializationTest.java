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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The suspended flag travels from the coordinator to the SSE stream bridges through the message broker; a lost
 * {@code false} would turn a stopped job into a suspended one.
 *
 * @author Ivica Cardic
 */
class SuspendedJobStatusSerializationTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    @Test
    void testSuspendedJobStatusApplicationEventKeepsTheSuspendedFlag() {
        JobStatusApplicationEvent readJobStatusApplicationEvent = jsonMapper.readValue(
            jsonMapper.writeValueAsString(JobStatusApplicationEvent.suspended(7L)), JobStatusApplicationEvent.class);

        assertThat(readJobStatusApplicationEvent.getJobId()).isEqualTo(7L);
        assertThat(readJobStatusApplicationEvent.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(readJobStatusApplicationEvent.isSuspended()).isTrue();
    }

    @Test
    void testSseStreamEventKeepsAFalseSuspendedMetadata() {
        SseStreamEvent sseStreamEvent = new SseStreamEvent(7L, SseStreamEvent.EVENT_TYPE_JOB_STATUS, "STOPPED");

        sseStreamEvent.putMetadata(SseStreamEvent.METADATA_SUSPENDED, false);

        SseStreamEvent readSseStreamEvent = jsonMapper.readValue(
            jsonMapper.writeValueAsString(sseStreamEvent), SseStreamEvent.class);

        assertThat(readSseStreamEvent.getMetadata(SseStreamEvent.METADATA_SUSPENDED)).isEqualTo(Boolean.FALSE);
    }
}
