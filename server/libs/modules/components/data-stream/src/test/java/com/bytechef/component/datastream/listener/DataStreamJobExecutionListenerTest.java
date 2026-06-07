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

package com.bytechef.component.datastream.listener;

import static com.bytechef.component.definition.datastream.ItemReader.SINCE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * @author Ivica Cardic
 */
class DataStreamJobExecutionListenerTest {

    @Test
    void testWritesLastSyncStartOnCompleted() {
        JobExecution jobExecution = newJobExecution();

        LocalDateTime startTime = LocalDateTime.of(2026, 5, 9, 10, 0, 0);

        jobExecution.setStartTime(startTime);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        new DataStreamJobExecutionListener().afterJob(jobExecution);

        ExecutionContext executionContext = jobExecution.getExecutionContext();

        assertThat(executionContext.containsKey(SINCE_KEY)).isTrue();

        long expectedMillis = startTime.toInstant(ZoneOffset.UTC)
            .toEpochMilli();

        assertThat(executionContext.getLong(SINCE_KEY)).isEqualTo(expectedMillis);
    }

    @Test
    void testNoWriteOnFailed() {
        JobExecution jobExecution = newJobExecution();

        jobExecution.setStartTime(LocalDateTime.of(2026, 5, 9, 10, 0, 0));
        jobExecution.setStatus(BatchStatus.FAILED);

        new DataStreamJobExecutionListener().afterJob(jobExecution);

        assertThat(jobExecution.getExecutionContext()
            .containsKey(SINCE_KEY)).isFalse();
    }

    @Test
    void testNoWriteWhenStartTimeNull() {
        JobExecution jobExecution = newJobExecution();

        jobExecution.setStatus(BatchStatus.COMPLETED);

        new DataStreamJobExecutionListener().afterJob(jobExecution);

        assertThat(jobExecution.getExecutionContext()
            .containsKey(SINCE_KEY)).isFalse();
    }

    private static JobExecution newJobExecution() {
        return new JobExecution(1L, new JobInstance(1L, "testJob"), new JobParameters());
    }
}
