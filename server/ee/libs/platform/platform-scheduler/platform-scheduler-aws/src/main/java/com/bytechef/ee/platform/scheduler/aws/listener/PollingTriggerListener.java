/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_POLLING_TRIGGER_QUEUE;

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

    @SqsListener(SCHEDULER_POLLING_TRIGGER_QUEUE)
    public void onSchedule(String message) {
        eventPublisher.publishEvent(new TriggerPollEvent(WorkflowExecutionId.parse(message)));
    }
}
