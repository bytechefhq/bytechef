/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.ONE_TIME_TASK_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_ONE_TIME_TASK_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Resumes a job when its suspend deadline passes. A message with the suspend's resume id resumes the job only while the
 * job still waits on that suspend; while the job has not reached the suspend yet the listener fails, so SQS delivers
 * the message again. A message without a resume id, or one whose resume id cannot be checked because no local
 * {@link JobResumeFacade} is available, resumes the job unconditionally.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OneTimeTaskListener {

    private static final AtomicBoolean UNCHECKED_RESUME_WARNED = new AtomicBoolean();

    private static final Logger log = LoggerFactory.getLogger(OneTimeTaskListener.class);

    private final ApplicationEventPublisher eventPublisher;
    private final @Nullable JobResumeFacade jobResumeFacade;

    public OneTimeTaskListener(
        ApplicationEventPublisher eventPublisher, @Nullable JobResumeFacade jobResumeFacade) {

        this.eventPublisher = eventPublisher;
        this.jobResumeFacade = jobResumeFacade;
    }

    @SqsListener(
        queueNames = SCHEDULER_ONE_TIME_TASK_QUEUE,
        id = ONE_TIME_TASK_LISTENER_ID,
        factory = SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY)
    public void onSchedule(String message) {
        String[] parts = message.split(AwsTriggerSchedulerConstants.SPLITTER, 2);

        if (!parts[0].startsWith(AwsTriggerSchedulerConstants.ONE_TIME_TASK_MESSAGE_PREFIX)) {
            throw new IllegalArgumentException("Unexpected one-time task message: " + parts[0]);
        }

        long jobId =
            Long.parseLong(parts[0].substring(AwsTriggerSchedulerConstants.ONE_TIME_TASK_MESSAGE_PREFIX.length()));

        String jobResumeId = parts.length > 1 ? getJobResumeId(parts[1]) : null;

        if (jobResumeId != null && jobResumeFacade != null) {
            try {
                JobResumeOutcome jobResumeOutcome = jobResumeFacade.resumeExpiredJob(jobResumeId);

                if (jobResumeOutcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
                    throw new IllegalStateException(
                        "Job " + jobId + " has not reached its suspend yet; its expiry is delivered again");
                }

                return;
            } catch (UnsupportedOperationException unsupportedOperationException) {
                if (UNCHECKED_RESUME_WARNED.compareAndSet(false, true)) {
                    log.warn(
                        "Expired suspends cannot be checked by this node's JobResumeFacade; jobs are resumed " +
                            "without checking that they still wait on the expired suspend");
                }
            }
        }

        eventPublisher.publishEvent(new ResumeJobEvent(jobId));
    }

    private static @Nullable String getJobResumeId(String continueParametersJson) {
        Map<String, Object> continueParameters = JsonUtils.readMap(continueParametersJson, Object.class);

        return continueParameters.get(JobResumeId.TIMEOUT_TASK_PARAMETER) instanceof String jobResumeId
            ? jobResumeId : null;
    }
}
