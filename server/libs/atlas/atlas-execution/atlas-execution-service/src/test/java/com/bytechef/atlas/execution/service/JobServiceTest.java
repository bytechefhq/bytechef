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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobServiceTest {

    private static final long JOB_ID = 7L;
    private static final Instant ORIGINAL_START_DATE = Instant.parse("2026-09-07T18:47:00Z");

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobService jobService = new JobServiceImpl(jobRepository);

    @Test
    void testResumeToStatusStartedRestartsFailedJobFromCurrentTask() {
        Job job = stubJob(Job.Status.FAILED);

        Job resumedJob = jobService.resumeToStatusStarted(JOB_ID);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(resumedJob.getCurrentTask()).isEqualTo(2);
        assertThat(resumedJob.getEndDate()).isNull();
        assertThat(resumedJob.getStartDate()).isAfter(ORIGINAL_START_DATE);

        verify(jobRepository).save(job);
    }

    @Test
    void testResumeToStatusStartedContinuesStoppedJob() {
        Job job = stubJob(Job.Status.STOPPED);

        Job resumedJob = jobService.resumeToStatusStarted(JOB_ID);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(resumedJob.getEndDate()).isNull();
        assertThat(resumedJob.getStartDate()).isEqualTo(ORIGINAL_START_DATE);

        verify(jobRepository).save(job);
    }

    @Test
    void testResumeToStatusStartedRejectsCompletedJob() {
        stubJob(Job.Status.COMPLETED);

        assertThatThrownBy(() -> jobService.resumeToStatusStarted(JOB_ID))
            .isInstanceOf(IllegalArgumentException.class);

        verify(jobRepository, never()).save(any());
    }

    private Job stubJob(Job.Status status) {
        Job job = new Job();

        job.setCurrentTask(2);
        job.setEndDate(Instant.now());
        job.setId(JOB_ID);
        job.setStartDate(ORIGINAL_START_DATE);
        job.setStatus(status);

        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        return job;
    }
}
