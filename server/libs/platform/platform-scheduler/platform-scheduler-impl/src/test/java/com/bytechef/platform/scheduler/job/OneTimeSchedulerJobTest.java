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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OneTimeSchedulerJobTest {

    private static final long JOB_ID = 42L;
    private static final JobKey JOB_KEY =
        JobKey.jobKey(String.valueOf(JOB_ID), QuartzTriggerSchedulerConstants.ONE_TIME_TASK);
    private static final String JOB_RESUME_ID = "resume-id";

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
    private final OneTimeSchedulerJob oneTimeSchedulerJob = new OneTimeSchedulerJob();

    @BeforeEach
    void beforeEach() {
        oneTimeSchedulerJob.setApplicationEventPublisher(eventPublisher);
    }

    @Test
    void testExecuteResumesTheExpiredSuspendThroughTheFacade() {
        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        oneTimeSchedulerJob.execute(createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID)));

        verify(jobResumeFacade).resumeExpiredJob(JOB_RESUME_ID);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testExecuteResumesTheJobWhenTheFacadeCannotResumeLocally() {
        when(jobResumeFacade.resumeExpiredJob(anyString())).thenThrow(new UnsupportedOperationException());

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        oneTimeSchedulerJob.execute(createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID)));

        verifyResumeJobEventPublished();
    }

    @Test
    void testExecuteResumesTheJobForATaskScheduledWithoutAResumeId() {
        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        oneTimeSchedulerJob.execute(createContext(Map.of()));

        verify(jobResumeFacade, never()).resumeExpiredJob(anyString());

        verifyResumeJobEventPublished();
    }

    @Test
    void testExecuteResumesTheJobWithoutAFacade() {
        oneTimeSchedulerJob.execute(createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID)));

        verifyResumeJobEventPublished();
    }

    @Test
    void testExecuteRetriesWhileTheJobHasNotReachedTheSuspend() throws SchedulerException {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        Scheduler scheduler = mock(Scheduler.class);

        oneTimeSchedulerJob.execute(
            createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID), scheduler, null));

        Trigger trigger = captureRetryTrigger(scheduler);

        assertEquals(JOB_KEY, trigger.getJobKey());
        assertEquals(1, trigger.getJobDataMap()
            .getInt(OneTimeSchedulerJob.RETRY_ATTEMPT));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testExecuteRetriesWhenResumingTheExpiredSuspendFails() throws SchedulerException {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenThrow(new IllegalStateException("database down"));

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        Scheduler scheduler = mock(Scheduler.class);

        oneTimeSchedulerJob.execute(
            createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID), scheduler, 3));

        Trigger trigger = captureRetryTrigger(scheduler);

        assertEquals(4, trigger.getJobDataMap()
            .getInt(OneTimeSchedulerJob.RETRY_ATTEMPT));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testExecuteStopsRetryingAfterTheLastAttempt() throws SchedulerException {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        Scheduler scheduler = mock(Scheduler.class);

        oneTimeSchedulerJob.execute(
            createContext(
                Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID), scheduler,
                OneTimeSchedulerJob.MAX_RETRY_ATTEMPTS));

        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    void testExecuteDoesNotRetryAResumeTheFacadeRejected() {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.GONE);

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        Scheduler scheduler = mock(Scheduler.class);

        oneTimeSchedulerJob.execute(
            createContext(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID), scheduler, null));

        verifyNoInteractions(scheduler);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testExecuteResumesTheJobWhenTheResumeIdCannotBeRead() {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("jobId", JOB_ID);
        jobDataMap.put(OneTimeSchedulerJob.CONTINUE_PARAMETERS, "{not json");

        JobExecutionContext context = mock(JobExecutionContext.class);

        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        oneTimeSchedulerJob.execute(context);

        verify(jobResumeFacade, never()).resumeExpiredJob(anyString());

        verifyResumeJobEventPublished();
    }

    @Test
    void testExecuteRetriesOnAQuartzSchedulerUntilTheJobReachesTheSuspend() throws SchedulerException {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED)
            .thenReturn(JobResumeOutcome.OK);

        oneTimeSchedulerJob.setJobResumeFacade(jobResumeFacade);

        Properties properties = new Properties();

        properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "OneTimeSchedulerJobTest");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");

        Scheduler scheduler = new StdSchedulerFactory(properties).getScheduler();

        scheduler.setJobFactory((triggerFiredBundle, jobScheduler) -> oneTimeSchedulerJob);

        try {
            scheduler.start();

            scheduler.scheduleJob(
                JobBuilder.newJob(OneTimeSchedulerJob.class)
                    .withIdentity(JOB_KEY)
                    .usingJobData("jobId", JOB_ID)
                    .usingJobData(
                        OneTimeSchedulerJob.CONTINUE_PARAMETERS,
                        JsonUtils.write(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID)))
                    .build(),
                TriggerBuilder.newTrigger()
                    .withIdentity(TriggerKey.triggerKey(JOB_KEY.getName(), JOB_KEY.getGroup()))
                    .startNow()
                    .build());

            verify(jobResumeFacade, timeout(10_000).times(2)).resumeExpiredJob(JOB_RESUME_ID);
        } finally {
            scheduler.shutdown(true);
        }

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testRetryDelayGrowsUpToTheMaximum() {
        assertEquals(Duration.ofSeconds(2), OneTimeSchedulerJob.getRetryDelay(0));
        assertEquals(Duration.ofSeconds(4), OneTimeSchedulerJob.getRetryDelay(1));
        assertEquals(Duration.ofMinutes(5), OneTimeSchedulerJob.getRetryDelay(OneTimeSchedulerJob.MAX_RETRY_ATTEMPTS));
    }

    private static Trigger captureRetryTrigger(Scheduler scheduler) throws SchedulerException {
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).scheduleJob(triggerCaptor.capture());

        return triggerCaptor.getValue();
    }

    private static JobExecutionContext createContext(
        Map<String, Object> continueParameters, Scheduler scheduler, @Nullable Integer retryAttempt) {

        JobExecutionContext context = createContext(continueParameters);

        if (retryAttempt != null) {
            context.getMergedJobDataMap()
                .put(OneTimeSchedulerJob.RETRY_ATTEMPT, retryAttempt.intValue());
        }

        when(context.getScheduler()).thenReturn(scheduler);
        when(context.getJobDetail()).thenReturn(
            JobBuilder.newJob(OneTimeSchedulerJob.class)
                .withIdentity(JOB_KEY)
                .build());

        return context;
    }

    private static JobExecutionContext createContext(Map<String, Object> continueParameters) {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("jobId", JOB_ID);

        if (!continueParameters.isEmpty()) {
            jobDataMap.put(OneTimeSchedulerJob.CONTINUE_PARAMETERS, JsonUtils.write(continueParameters));
        }

        JobExecutionContext context = mock(JobExecutionContext.class);

        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        return context;
    }

    private void verifyResumeJobEventPublished() {
        ArgumentCaptor<ResumeJobEvent> resumeJobEventCaptor = ArgumentCaptor.forClass(ResumeJobEvent.class);

        verify(eventPublisher).publishEvent(resumeJobEventCaptor.capture());

        ResumeJobEvent resumeJobEvent = resumeJobEventCaptor.getValue();

        assertEquals(JOB_ID, resumeJobEvent.getJobId());
    }
}
