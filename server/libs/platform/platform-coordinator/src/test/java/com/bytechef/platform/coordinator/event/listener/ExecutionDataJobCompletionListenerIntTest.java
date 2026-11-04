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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformCoordinatorIntTestConfiguration.class, properties = "bytechef.data-storage.provider=jdbc")
@Timeout(30)
class ExecutionDataJobCompletionListenerIntTest {

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    private ExecutionDataJobCompletionListener listener;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM data_entry");
        jdbcTemplate.update("UPDATE job SET parent_task_execution_id = NULL");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        listener = new ExecutionDataJobCompletionListener(dataStorage, jobService);
    }

    @Test
    void testDeletesTheCurrentExecutionValuesOfACompletedJob() {
        long jobId = saveJob(Status.COMPLETED, null, Map.of());
        long otherJobId = saveJob(Status.STARTED, null, Map.of());

        putCurrentExecutionValue(jobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(jobId, PlatformType.EMBEDDED);
        putCurrentExecutionValue(otherJobId, PlatformType.AUTOMATION);

        dataStorage.put(
            "test", DataStorageScope.WORKFLOW, String.valueOf(jobId), "key", "value", 0, PlatformType.AUTOMATION);

        listener.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        assertThat(hasCurrentExecutionValue(jobId, PlatformType.AUTOMATION)).isFalse();
        assertThat(hasCurrentExecutionValue(jobId, PlatformType.EMBEDDED)).isFalse();
        assertThat(hasCurrentExecutionValue(otherJobId, PlatformType.AUTOMATION)).isTrue();
        assertThat(
            dataStorage.fetch(
                "test", DataStorageScope.WORKFLOW, String.valueOf(jobId), "key", 0, PlatformType.AUTOMATION))
                    .isPresent();
    }

    @Test
    void testDeletesTheValuesOfTheCompletedSubflowJobsOfACompletedJob() {
        long jobId = saveJob(Status.COMPLETED, null, Map.of());
        long childJobId = saveJob(Status.COMPLETED, saveTaskExecution(jobId), Map.of());
        long grandchildJobId = saveJob(Status.COMPLETED, saveTaskExecution(childJobId), Map.of());
        long failedChildJobId = saveJob(Status.FAILED, saveTaskExecution(jobId), Map.of());

        putCurrentExecutionValue(jobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(childJobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(grandchildJobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(failedChildJobId, PlatformType.AUTOMATION);

        listener.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        assertThat(hasCurrentExecutionValue(jobId, PlatformType.AUTOMATION)).isFalse();
        assertThat(hasCurrentExecutionValue(childJobId, PlatformType.AUTOMATION)).isFalse();
        assertThat(hasCurrentExecutionValue(grandchildJobId, PlatformType.AUTOMATION)).isFalse();
        assertThat(hasCurrentExecutionValue(failedChildJobId, PlatformType.AUTOMATION)).isTrue();
    }

    @Test
    void testKeepsTheValuesOfACompletedSubflowJobUntilItsTopLevelJobCompletes() {
        long jobId = saveJob(Status.STARTED, null, Map.of());
        long childJobId = saveJob(Status.COMPLETED, saveTaskExecution(jobId), Map.of());

        putCurrentExecutionValue(childJobId, PlatformType.AUTOMATION);

        listener.onApplicationEvent(new JobStatusApplicationEvent(childJobId, Status.COMPLETED));

        assertThat(hasCurrentExecutionValue(childJobId, PlatformType.AUTOMATION)).isTrue();
    }

    @Test
    void testKeepsTheValuesOfAJobThatDidNotComplete() {
        long failedJobId = saveJob(Status.FAILED, null, Map.of());
        long stoppedJobId = saveJob(Status.STOPPED, null, Map.of());
        long suspendedJobId = saveJob(Status.STOPPED, null, Map.of());

        putCurrentExecutionValue(failedJobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(stoppedJobId, PlatformType.AUTOMATION);
        putCurrentExecutionValue(suspendedJobId, PlatformType.AUTOMATION);

        listener.onApplicationEvent(new JobStatusApplicationEvent(failedJobId, Status.FAILED));
        listener.onApplicationEvent(new JobStatusApplicationEvent(stoppedJobId, Status.STOPPED));
        listener.onApplicationEvent(JobStatusApplicationEvent.suspended(suspendedJobId));
        listener.onApplicationEvent(new TaskStartedApplicationEvent(failedJobId, 7L));

        assertThat(hasCurrentExecutionValue(failedJobId, PlatformType.AUTOMATION)).isTrue();
        assertThat(hasCurrentExecutionValue(stoppedJobId, PlatformType.AUTOMATION)).isTrue();
        assertThat(hasCurrentExecutionValue(suspendedJobId, PlatformType.AUTOMATION)).isTrue();
    }

    @Test
    void testKeepsTheValuesOfACompletedEditorJob() {
        long jobId = saveJob(Status.COMPLETED, null, Map.of(MetadataConstants.EDITOR_ENVIRONMENT, true));

        putCurrentExecutionValue(jobId, PlatformType.AUTOMATION);

        listener.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        assertThat(hasCurrentExecutionValue(jobId, PlatformType.AUTOMATION)).isTrue();
    }

    @Test
    void testDoesNothingWithoutADataStorage() {
        long jobId = saveJob(Status.COMPLETED, null, Map.of());

        putCurrentExecutionValue(jobId, PlatformType.AUTOMATION);

        ExecutionDataJobCompletionListener listenerWithoutDataStorage = new ExecutionDataJobCompletionListener(
            null, jobService);

        listenerWithoutDataStorage.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        assertThat(hasCurrentExecutionValue(jobId, PlatformType.AUTOMATION)).isTrue();
    }

    private boolean hasCurrentExecutionValue(long jobId, PlatformType type) {
        Optional<Object> value = dataStorage.fetch(
            "test", DataStorageScope.CURRENT_EXECUTION, String.valueOf(jobId), type.name(), 0, type);

        return value.isPresent();
    }

    private void putCurrentExecutionValue(long jobId, PlatformType type) {
        dataStorage.put(
            "test", DataStorageScope.CURRENT_EXECUTION, String.valueOf(jobId), type.name(), "value", 0, type);
    }

    private long saveJob(Status status, @Nullable Long parentTaskExecutionId, Map<String, Object> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);
        job.setParentTaskExecutionId(parentTaskExecutionId);
        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private long saveTaskExecution(long jobId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "subflow1", "type", "subflow/v1")))
            .build();

        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionRepository.save(taskExecution);

        return Objects.requireNonNull(taskExecution.getId());
    }
}
