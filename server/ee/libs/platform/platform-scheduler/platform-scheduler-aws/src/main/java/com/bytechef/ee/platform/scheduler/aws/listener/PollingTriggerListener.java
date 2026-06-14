/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.POLLING_TRIGGER_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_POLLING_TRIGGER_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class PollingTriggerListener {

    private final ApplicationEventPublisher eventPublisher;

    public PollingTriggerListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @SqsListener(
        queueNames = SCHEDULER_POLLING_TRIGGER_QUEUE,
        id = POLLING_TRIGGER_LISTENER_ID,
        factory = SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY)
    public void onSchedule(String message) {
        eventPublisher.publishEvent(new TriggerPollEvent(WorkflowExecutionId.parse(message)));
    }
}
