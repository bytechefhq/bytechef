/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OneTimeTaskListenerTest {

    private static final long JOB_ID = 42L;
    private static final String JOB_RESUME_ID = "resume-id";

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

    @Test
    void testOnScheduleResumesTheExpiredSuspendThroughTheFacade() {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.OK);

        new OneTimeTaskListener(eventPublisher, jobResumeFacade).onSchedule(createMessage(true));

        verify(jobResumeFacade).resumeExpiredJob(JOB_RESUME_ID);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testOnScheduleFailsSoTheMessageIsDeliveredAgainWhileTheJobHasNotReachedTheSuspend() {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        OneTimeTaskListener oneTimeTaskListener = new OneTimeTaskListener(eventPublisher, jobResumeFacade);

        String message = createMessage(true);

        assertThrows(IllegalStateException.class, () -> oneTimeTaskListener.onSchedule(message));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testOnScheduleIgnoresAnExpiryTheFacadeRejected() {
        when(jobResumeFacade.resumeExpiredJob(JOB_RESUME_ID)).thenReturn(JobResumeOutcome.GONE);

        new OneTimeTaskListener(eventPublisher, jobResumeFacade).onSchedule(createMessage(true));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testOnScheduleResumesTheJobWhenTheFacadeCannotResumeLocally() {
        when(jobResumeFacade.resumeExpiredJob(anyString())).thenThrow(new UnsupportedOperationException());

        new OneTimeTaskListener(eventPublisher, jobResumeFacade).onSchedule(createMessage(true));

        verifyResumeJobEventPublished();
    }

    @Test
    void testOnScheduleResumesTheJobForAMessageWithoutAResumeId() {
        new OneTimeTaskListener(eventPublisher, jobResumeFacade).onSchedule(createMessage(false));

        verify(jobResumeFacade, never()).resumeExpiredJob(anyString());

        verifyResumeJobEventPublished();
    }

    @Test
    void testOnScheduleResumesTheJobWithoutAFacade() {
        new OneTimeTaskListener(eventPublisher, null).onSchedule(createMessage(true));

        verifyResumeJobEventPublished();
    }

    @Test
    void testOnScheduleRejectsAnUnexpectedMessage() {
        OneTimeTaskListener oneTimeTaskListener = new OneTimeTaskListener(eventPublisher, jobResumeFacade);

        assertThrows(IllegalArgumentException.class, () -> oneTimeTaskListener.onSchedule("poll:42"));
    }

    private static String createMessage(boolean withJobResumeId) {
        String message = AwsTriggerSchedulerConstants.ONE_TIME_TASK_MESSAGE_PREFIX + JOB_ID;

        if (!withJobResumeId) {
            return message;
        }

        return message + AwsTriggerSchedulerConstants.SPLITTER +
            JsonUtils.write(Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, JOB_RESUME_ID));
    }

    private void verifyResumeJobEventPublished() {
        ArgumentCaptor<ResumeJobEvent> resumeJobEventCaptor = ArgumentCaptor.forClass(ResumeJobEvent.class);

        verify(eventPublisher).publishEvent(resumeJobEventCaptor.capture());

        ResumeJobEvent resumeJobEvent = resumeJobEventCaptor.getValue();

        assertEquals(JOB_ID, resumeJobEvent.getJobId());
    }
}
