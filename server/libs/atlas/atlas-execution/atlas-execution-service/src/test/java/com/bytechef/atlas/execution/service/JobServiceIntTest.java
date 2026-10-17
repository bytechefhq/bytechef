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

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.config.AtlasExecutionServiceIntTestConfiguration;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.error.ExecutionError;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AtlasExecutionServiceIntTestConfiguration.class)
public class JobServiceIntTest {

    private static final Instant ORIGINAL_START_DATE = Instant.parse("2026-09-07T18:47:00Z");

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Test
    public void testGetChildJobsLooksUpJobsByParentTaskExecutionIds() {
        Job parentJob = jobRepository.save(newJob(Job.Status.STARTED));

        long parentJobId = Validate.notNull(parentJob.getId(), "id");

        long firstTaskExecutionId = saveTaskExecution(parentJobId);
        long secondTaskExecutionId = saveTaskExecution(parentJobId);
        long unrelatedTaskExecutionId = saveTaskExecution(parentJobId);

        long firstChildJobId = saveChildJob(firstTaskExecutionId);
        long secondChildJobId = saveChildJob(secondTaskExecutionId);

        saveChildJob(unrelatedTaskExecutionId);

        List<Job> childJobs = jobService.getChildJobs(List.of(firstTaskExecutionId, secondTaskExecutionId));

        assertThat(childJobs)
            .extracting(Job::getId)
            .containsExactlyInAnyOrder(firstChildJobId, secondChildJobId);
        assertThat(childJobs)
            .extracting(Job::getParentTaskExecutionId)
            .containsExactlyInAnyOrder(firstTaskExecutionId, secondTaskExecutionId);
    }

    @Test
    public void testResumeToStatusStartedRestartsFailedJobFromCurrentTask() {
        long jobId = saveResumableJob(Job.Status.FAILED);

        Job failedJob = jobRepository.findById(jobId)
            .orElseThrow();

        failedJob.setError(new ExecutionError("The pending approval expired", List.of()));

        jobRepository.save(failedJob);

        Job resumedJob = jobService.resumeToStatusStarted(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);

        Job persistedJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(persistedJob.getCurrentTask()).isEqualTo(2);
        assertThat(persistedJob.getEndDate()).isNull();
        assertThat(persistedJob.getError()).isNull();
        assertThat(persistedJob.getStartDate()).isAfter(ORIGINAL_START_DATE);
    }

    @Test
    public void testResumeToStatusStartedContinuesStoppedJob() {
        long jobId = saveResumableJob(Job.Status.STOPPED);

        Job resumedJob = jobService.resumeToStatusStarted(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);

        Job persistedJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(persistedJob.getEndDate()).isNull();
        assertThat(persistedJob.getStartDate()).isEqualTo(ORIGINAL_START_DATE);
    }

    @Test
    public void testResumeToStatusStartedRejectsCompletedJob() {
        long jobId = saveResumableJob(Job.Status.COMPLETED);

        Job originalJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThatThrownBy(() -> jobService.resumeToStatusStarted(jobId))
            .isInstanceOf(IllegalArgumentException.class);

        Job persistedJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.COMPLETED);
        assertThat(persistedJob.getEndDate()).isEqualTo(originalJob.getEndDate());
        assertThat(persistedJob.getLastModifiedDate()).isEqualTo(originalJob.getLastModifiedDate());
    }

    @Test
    public void testResumeToStatusStartedResumesSuspendedSubflowChild() {
        long jobId = saveResumableChildJob(Job.Status.STOPPED);

        Job resumedJob = jobService.resumeToStatusStarted(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);

        Job persistedJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(persistedJob.getEndDate()).isNull();
    }

    @Test
    public void testResumeToStatusStartedRejectsFailedSubflowChild() {
        long jobId = saveResumableChildJob(Job.Status.FAILED);

        Job originalJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThatThrownBy(() -> jobService.resumeToStatusStarted(jobId))
            .isInstanceOf(IllegalArgumentException.class);

        Job persistedJob = jobRepository.findById(jobId)
            .orElseThrow();

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(persistedJob.getLastModifiedDate()).isEqualTo(originalJob.getLastModifiedDate());
    }

    private static Job newJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        return job;
    }

    private long saveChildJob(long parentTaskExecutionId) {
        Job childJob = newJob(Job.Status.STARTED);

        childJob.setParentTaskExecutionId(parentTaskExecutionId);

        childJob = jobRepository.save(childJob);

        return Validate.notNull(childJob.getId(), "id");
    }

    private long saveResumableChildJob(Job.Status status) {
        Job parentJob = jobRepository.save(newJob(Job.Status.STOPPED));

        long parentTaskExecutionId = saveTaskExecution(Validate.notNull(parentJob.getId(), "id"));

        Job job = newJob(status);

        job.setEndDate(Instant.now());
        job.setParentTaskExecutionId(parentTaskExecutionId);
        job.setStartDate(ORIGINAL_START_DATE);

        job = jobRepository.save(job);

        return Validate.notNull(job.getId(), "id");
    }

    private long saveResumableJob(Job.Status status) {
        Job job = newJob(status);

        job.setCurrentTask(2);
        job.setEndDate(Instant.now());
        job.setStartDate(ORIGINAL_START_DATE);

        job = jobRepository.save(job);

        return Validate.notNull(job.getId(), "id");
    }

    private long saveTaskExecution(long jobId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "test/v1/noop")))
            .build();

        taskExecution = taskExecutionRepository.save(taskExecution);

        return Validate.notNull(taskExecution.getId(), "id");
    }
}
