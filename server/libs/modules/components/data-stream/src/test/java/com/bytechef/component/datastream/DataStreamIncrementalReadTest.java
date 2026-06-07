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

package com.bytechef.component.datastream;

import static com.bytechef.component.definition.datastream.ItemReader.SINCE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.datastream.listener.DataStreamJobExecutionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Round-trip test for the incremental-sync flow: a stub {@link ItemStreamReader} that flips between full-pull and
 * incremental behavior based on the {@link com.bytechef.component.definition.datastream.ItemReader#SINCE_KEY} value in
 * the per-step ExecutionContext, paired with {@link DataStreamJobExecutionListener} writing the high-water mark on
 * completion.
 *
 * <p>
 * Two scenarios are exercised:
 *
 * <ul>
 * <li>First run (no {@code datastream.since} JobParameter): the stub emits all 3 records, and the listener writes the
 * SINCE_KEY into the JobExecution's ExecutionContext.</li>
 * <li>Second run (with {@code datastream.since} = 1000L): the stub observes the value via its {@code ExecutionContext}
 * and emits only 1 record.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
class DataStreamIncrementalReadTest {

    private static final List<Map<String, Object>> FULL_PULL_RECORDS = List.of(
        Map.of("id", 1, "name", "alpha"),
        Map.of("id", 2, "name", "beta"),
        Map.of("id", 3, "name", "gamma"));

    private static final List<Map<String, Object>> INCREMENTAL_RECORDS = List.of(
        Map.of("id", 4, "name", "delta"));

    @Test
    void testFullPullEmitsAllRecordsAndListenerWritesSinceKey() throws Exception {
        StubReader reader = new StubReader();
        StubWriter writer = new StubWriter();

        JobExecution jobExecution = runJob(reader, writer, new JobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(writer.getWritten()).hasSize(3);
        assertThat(writer.getWritten()).containsExactlyElementsOf(FULL_PULL_RECORDS);

        ExecutionContext jobContext = jobExecution.getExecutionContext();

        assertThat(jobContext.containsKey(SINCE_KEY)).isTrue();
        assertThat(jobContext.getLong(SINCE_KEY)).isPositive();
    }

    @Test
    void testIncrementalRunFiltersByObservedSinceValue() throws Exception {
        StubReader reader = new StubReader();
        StubWriter writer = new StubWriter();

        long sinceMillis = 1000L;
        JobParameters jobParameters = new JobParameters(
            Set.of(new JobParameter<>(SINCE_KEY, sinceMillis, Long.class, true)));

        JobExecution jobExecution = runJob(reader, writer, jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(writer.getWritten()).hasSize(1);
        assertThat(writer.getWritten()).containsExactlyElementsOf(INCREMENTAL_RECORDS);
        assertThat(reader.getObservedSince()).isEqualTo(sinceMillis);
    }

    private JobExecution runJob(
        StubReader reader, StubWriter writer, JobParameters jobParameters) throws Exception {

        JobRepository jobRepository = new ResourcelessJobRepository();
        PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

        Step step = new StepBuilder("incrementalStep", jobRepository)
            .<Map<String, Object>, Map<String, Object>>chunk(10, transactionManager)
            .reader(new SinceCopyingReader(reader, jobParameters))
            .writer(writer)
            .build();

        Job job = new JobBuilder("incrementalJob", jobRepository)
            .listener(new DataStreamJobExecutionListener())
            .start(step)
            .build();

        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();

        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();

        return jobLauncher.run(job, jobParameters);
    }

    /**
     * Mirrors what {@code ItemStreamReaderDelegate.open} does in production: copies a {@code datastream.since}
     * JobParameter, when present, into the per-step {@link ExecutionContext} under
     * {@link com.bytechef.component.definition.datastream.ItemReader#SINCE_KEY} before delegating to the wrapped
     * reader.
     */
    private static final class SinceCopyingReader implements ItemStreamReader<Map<String, Object>> {

        private final StubReader delegate;
        private final JobParameters jobParameters;

        SinceCopyingReader(StubReader delegate, JobParameters jobParameters) {
            this.delegate = delegate;
            this.jobParameters = jobParameters;
        }

        @Override
        public void open(ExecutionContext executionContext) throws ItemStreamException {
            JobParameter<?> sinceParameter = jobParameters.getParameter(SINCE_KEY);

            if (sinceParameter != null && sinceParameter.value() instanceof Long longValue) {
                executionContext.putLong(SINCE_KEY, longValue);
            }

            delegate.open(executionContext);
        }

        @Override
        public Map<String, Object> read() {
            return delegate.read();
        }
    }

    /**
     * Stub reader: emits {@link #FULL_PULL_RECORDS} when the per-step ExecutionContext does not contain
     * {@link com.bytechef.component.definition.datastream.ItemReader#SINCE_KEY}; emits {@link #INCREMENTAL_RECORDS}
     * otherwise. Records the observed value for later assertion.
     */
    private static final class StubReader {

        private List<Map<String, Object>> records = List.of();
        private int index;
        private Long observedSince;

        public void open(ExecutionContext executionContext) {
            if (executionContext.containsKey(SINCE_KEY)) {
                this.observedSince = executionContext.getLong(SINCE_KEY);
                this.records = INCREMENTAL_RECORDS;
            } else {
                this.records = FULL_PULL_RECORDS;
            }

            this.index = 0;
        }

        public Map<String, Object> read() {
            if (index >= records.size()) {
                return null;
            }

            return records.get(index++);
        }

        public Long getObservedSince() {
            return observedSince;
        }
    }

    private static final class StubWriter implements ItemWriter<Map<String, Object>> {

        private final List<Map<String, Object>> written = new ArrayList<>();

        @Override
        public void write(Chunk<? extends Map<String, Object>> chunk) {
            written.addAll(chunk.getItems());
        }

        public List<Map<String, Object>> getWritten() {
            return written;
        }
    }
}
