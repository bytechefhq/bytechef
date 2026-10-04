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

package com.bytechef.platform.scheduler.job;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Resumes a job when its suspend deadline passes. A task scheduled with the suspend's resume id resumes the job only
 * while the job still waits on that suspend, and retries with a growing delay while the job has not reached the suspend
 * yet or the resume fails. A task without a resume id (scheduled before resume ids were stored), or one whose resume id
 * cannot be checked because no local {@link JobResumeFacade} is available, resumes the job unconditionally.
 *
 * @author Ivica Cardic
 */
public class OneTimeSchedulerJob implements Job {

    public static final String CONTINUE_PARAMETERS = "continueParameters";

    static final int MAX_RETRY_ATTEMPTS = 10;
    static final String RETRY_ATTEMPT = "retryAttempt";

    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);
    private static final AtomicBoolean UNCHECKED_RESUME_WARNED = new AtomicBoolean();

    private static final Logger log = LoggerFactory.getLogger(OneTimeSchedulerJob.class);

    private ApplicationEventPublisher eventPublisher;
    private @Nullable JobResumeFacade jobResumeFacade;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        long jobId = jobDataMap.getLong("jobId");

        String jobResumeId = getJobResumeId(jobDataMap, jobId);

        if (jobResumeId != null && jobResumeFacade != null) {
            try {
                JobResumeOutcome jobResumeOutcome = jobResumeFacade.resumeExpiredJob(jobResumeId);

                if (jobResumeOutcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
                    scheduleRetry(context, jobId, null);
                }

                return;
            } catch (UnsupportedOperationException unsupportedOperationException) {
                if (UNCHECKED_RESUME_WARNED.compareAndSet(false, true)) {
                    log.warn(
                        "Expired suspends cannot be checked by this node's JobResumeFacade; jobs are resumed " +
                            "without checking that they still wait on the expired suspend");
                }
            } catch (RuntimeException exception) {
                scheduleRetry(context, jobId, exception);

                return;
            }
        }

        eventPublisher.publishEvent(new ResumeJobEvent(jobId));
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Autowired(required = false)
    public void setJobResumeFacade(JobResumeFacade jobResumeFacade) {
        this.jobResumeFacade = jobResumeFacade;
    }

    static Duration getRetryDelay(int retryAttempt) {
        Duration retryDelay = INITIAL_RETRY_DELAY.multipliedBy(1L << Math.min(retryAttempt, 16));

        return retryDelay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : retryDelay;
    }

    private static @Nullable String getJobResumeId(JobDataMap jobDataMap, long jobId) {
        if (!jobDataMap.containsKey(CONTINUE_PARAMETERS)) {
            return null;
        }

        try {
            Map<String, Object> continueParameters = JsonUtils.readMap(
                jobDataMap.getString(CONTINUE_PARAMETERS), Object.class);

            return continueParameters.get(JobResumeId.TIMEOUT_TASK_PARAMETER) instanceof String jobResumeId
                ? jobResumeId : null;
        } catch (RuntimeException exception) {
            log.warn("Unable to read the resume id of job {}'s expired suspend", jobId, exception);

            return null;
        }
    }

    private static void scheduleRetry(JobExecutionContext context, long jobId, @Nullable Exception exception) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        int retryAttempt = jobDataMap.containsKey(RETRY_ATTEMPT) ? jobDataMap.getInt(RETRY_ATTEMPT) : 0;

        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            log.error(
                "Giving up resuming job {} after its suspend expired; tried {} times", jobId, retryAttempt + 1,
                exception);

            return;
        }

        Duration retryDelay = getRetryDelay(retryAttempt);
        Instant now = Instant.now();

        JobDetail jobDetail = context.getJobDetail();

        JobKey jobKey = jobDetail.getKey();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(
                TriggerKey.triggerKey(jobKey.getName() + "_retry_" + (retryAttempt + 1), jobKey.getGroup()))
            .forJob(jobKey)
            .usingJobData(RETRY_ATTEMPT, retryAttempt + 1)
            .startAt(Date.from(now.plus(retryDelay)))
            .build();

        try {
            Scheduler scheduler = context.getScheduler();

            scheduler.scheduleJob(trigger);
        } catch (SchedulerException schedulerException) {
            log.error("Unable to retry resuming job {} after its suspend expired", jobId, schedulerException);

            return;
        }

        if (exception == null) {
            log.info("Job {} has not reached its suspend yet; retrying its expiry in {}", jobId, retryDelay);
        } else {
            log.warn("Unable to resume job {} after its suspend expired; retrying in {}", jobId, retryDelay, exception);
        }
    }
}
