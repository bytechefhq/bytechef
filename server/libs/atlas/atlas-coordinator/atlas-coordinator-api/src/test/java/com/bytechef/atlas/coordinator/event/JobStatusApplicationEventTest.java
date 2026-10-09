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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class JobStatusApplicationEventTest {

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
}
