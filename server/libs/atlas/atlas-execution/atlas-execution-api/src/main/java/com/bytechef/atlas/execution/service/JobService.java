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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface JobService {

    Job create(JobParametersDTO jobParametersDTO, Workflow workflow);

    void deleteJob(long id);

    Optional<Job> fetchJob(Long id);

    Optional<Job> fetchLastJob();

    Optional<Job> fetchLastWorkflowJob(String workflowId);

    Optional<Job> fetchLastWorkflowJob(List<String> workflowIds);

    List<Long> getChildJobIds(long parentJobId);

    Job getJob(long id);

    List<Job> getJobs(List<Long> ids);

    /**
     * Returns jobs with the given status whose last-modified timestamp is older than the given instant. Used by orphan
     * detection to find jobs wedged in STARTED with no live task execution.
     */
    List<Job> getStaleJobs(Job.Status status, java.time.Instant lastModifiedDateBefore);

    /**
     * Jobs in the given status started before the cutoff — used by the per-run execution timeout monitor. Remote-client
     * deployments may not support this query and throw {@link UnsupportedOperationException}.
     */
    List<Job> getLongRunningJobs(Job.Status status, java.time.Instant startDateBefore);

    /**
     * Terminal jobs whose end date is older than the cutoff — used by the retention purge monitor. Only terminal jobs
     * carry an end date, so status filtering is unnecessary.
     */
    List<Job> getEndedJobs(java.time.Instant endDateBefore);

    Page<Job> getJobsPage(int pageNumber);

    Job getTaskExecutionJob(long taskExecutionId);

    Job resumeToStatusStarted(long id);

    /**
     * Atomically claims a suspended run for resume by transitioning the given (already-read) job from STOPPED to
     * STARTED under its optimistic-lock version. Returns {@code true} for the single winner and {@code false} when the
     * claim is lost — because a concurrent resolution already claimed it or the expiry sweep flipped it to FAILED,
     * either of which bumps the version. The lost-claim case is reported as a return value rather than a thrown
     * {@link org.springframework.dao.OptimisticLockingFailureException} so a transactional caller can react without the
     * shared transaction being marked rollback-only.
     */
    boolean tryClaimResume(Job job);

    Job setStatusToStarted(long id);

    Job setStatusToStopped(long id);

    Job update(Job job);
}
