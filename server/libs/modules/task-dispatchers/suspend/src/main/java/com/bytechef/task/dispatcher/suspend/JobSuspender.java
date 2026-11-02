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

package com.bytechef.task.dispatcher.suspend;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Stops a job on the suspend one of its tasks asked for. A job waits on one suspend at a time: while it waits on one, a
 * suspend another branch asks for is queued and becomes the job's suspend once the one it waits on resumes.
 *
 * @author Ivica Cardic
 */
public class JobSuspender {

    private static final Logger log = LoggerFactory.getLogger(JobSuspender.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskStateService taskStateService;
    private final @Nullable TriggerScheduler triggerScheduler;

    @SuppressFBWarnings("EI")
    public JobSuspender(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskStateService taskStateService,
        @Nullable TriggerScheduler triggerScheduler) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskStateService = taskStateService;
        this.triggerScheduler = triggerScheduler;
    }

    /**
     * Stops the job on the task's suspend, or queues the suspend when the job already waits on another task's suspend.
     */
    public void suspend(long jobId, long taskExecutionId, String jobResumeIdString, Suspend suspend) {
        Job job = jobService.getJob(jobId);

        if (isWaitingOnAnotherTask(job, taskExecutionId)) {
            List<Map<String, Object>> queuedSuspends = new ArrayList<>(getQueuedSuspends(job));

            queuedSuspends.add(
                Map.of(
                    MetadataConstants.JOB_RESUME_ID, jobResumeIdString,
                    MetadataConstants.TASK_EXECUTION_RESUME_ID, taskExecutionId));

            Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

            jobMetadata.put(MetadataConstants.QUEUED_SUSPENDS, queuedSuspends);

            job.setMetadata(jobMetadata);

            jobService.update(job);

            restoreTimeout(jobId, (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID));

            if (log.isDebugEnabled()) {
                log.debug(
                    "Job id={} already waits on a suspend, queued the suspend of task execution id={}", jobId,
                    taskExecutionId);
            }

            return;
        }

        stop(job, taskExecutionId, jobResumeIdString, suspend);
    }

    /**
     * Stops the job on the oldest queued suspend, if any, once the suspend it waited on has resumed.
     */
    public void suspendOnNextQueuedSuspend(long jobId) {
        Job job = jobService.getJob(jobId);

        List<Map<String, Object>> queuedSuspends = getQueuedSuspends(job);

        if (queuedSuspends.isEmpty()) {
            return;
        }

        Map<String, Object> queuedSuspend = queuedSuspends.getFirst();

        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        if (queuedSuspends.size() == 1) {
            jobMetadata.remove(MetadataConstants.QUEUED_SUSPENDS);
        } else {
            jobMetadata.put(
                MetadataConstants.QUEUED_SUSPENDS, new ArrayList<>(queuedSuspends.subList(1, queuedSuspends.size())));
        }

        job.setMetadata(jobMetadata);

        String jobResumeIdString = (String) queuedSuspend.get(MetadataConstants.JOB_RESUME_ID);

        Optional<Suspend> suspendOptional = taskStateService.fetchValue(JobResumeId.parse(jobResumeIdString));

        if (suspendOptional.isEmpty()) {
            log.warn("No suspend state found for the queued jobResumeId={} of job id={}", jobResumeIdString, jobId);
        }

        Number taskExecutionId = (Number) queuedSuspend.get(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        Suspend suspend = suspendOptional.orElseGet(() -> new Suspend(Map.of(), null));

        stop(job, taskExecutionId.longValue(), jobResumeIdString, suspend);

        Instant expiresAt = suspend.expiresAt();

        if (expiresAt != null) {
            scheduleTimeout(jobId, jobResumeIdString, expiresAt);
        }
    }

    private void restoreTimeout(long jobId, @Nullable String activeJobResumeIdString) {
        if (triggerScheduler == null || activeJobResumeIdString == null) {
            return;
        }

        Optional<Suspend> activeSuspendOptional = taskStateService.fetchValue(
            JobResumeId.parse(activeJobResumeIdString));

        Instant expiresAt = activeSuspendOptional.map(Suspend::expiresAt)
            .orElse(null);

        if (expiresAt == null) {
            try {
                triggerScheduler.cancelOneTimeTask(jobId);
            } catch (Exception exception) {
                log.warn("Failed to cancel the suspend timeout task of job {}", jobId, exception);
            }

            return;
        }

        scheduleTimeout(jobId, activeJobResumeIdString, expiresAt);
    }

    private void scheduleTimeout(long jobId, String jobResumeIdString, Instant expiresAt) {
        if (triggerScheduler == null) {
            return;
        }

        try {
            triggerScheduler.scheduleOneTimeTask(
                expiresAt, Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, jobResumeIdString), jobId);
        } catch (Exception exception) {
            log.warn("Failed to schedule the suspend timeout task of job {}", jobId, exception);
        }
    }

    private void stop(Job job, long taskExecutionId, String jobResumeIdString, Suspend suspend) {
        long jobId = Objects.requireNonNull(job.getId());

        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.put(MetadataConstants.JOB_RESUME_ID, jobResumeIdString);
        jobMetadata.put(
            MetadataConstants.APPROVAL_RESUME,
            MapUtils.getBoolean(suspend.continueParameters(), MetadataConstants.APPROVAL_RESUME, false));
        jobMetadata.put(
            MetadataConstants.STREAMING_RESUME,
            MapUtils.getBoolean(suspend.continueParameters(), MetadataConstants.STREAMING_RESUME, false));
        jobMetadata.put(
            MetadataConstants.SUBFLOW_RESUME,
            MapUtils.getBoolean(suspend.continueParameters(), MetadataConstants.SUBFLOW_RESUME, false));
        jobMetadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, taskExecutionId);

        job = jobService.getJob(jobId);

        job.setMetadata(jobMetadata);

        jobService.update(job);

        jobService.setStatusToStopped(jobId);

        eventPublisher.publishEvent(JobStatusApplicationEvent.suspended(jobId));
    }

    private static List<Map<String, Object>> getQueuedSuspends(Job job) {
        if (!(job.getMetadata(MetadataConstants.QUEUED_SUSPENDS) instanceof List<?> queuedSuspends)) {
            return List.of();
        }

        List<Map<String, Object>> typedQueuedSuspends = new ArrayList<>();

        for (Object queuedSuspend : queuedSuspends) {
            if (queuedSuspend instanceof Map<?, ?> queuedSuspendMap) {
                Map<String, Object> typedQueuedSuspend = new HashMap<>();

                queuedSuspendMap.forEach((key, value) -> typedQueuedSuspend.put(String.valueOf(key), value));

                typedQueuedSuspends.add(typedQueuedSuspend);
            }
        }

        return typedQueuedSuspends;
    }

    private static boolean isWaitingOnAnotherTask(Job job, long taskExecutionId) {
        if (job.getMetadata(MetadataConstants.JOB_RESUME_ID) == null) {
            return false;
        }

        return job.getMetadata(MetadataConstants.TASK_EXECUTION_RESUME_ID) instanceof Number number &&
            number.longValue() != taskExecutionId;
    }
}
