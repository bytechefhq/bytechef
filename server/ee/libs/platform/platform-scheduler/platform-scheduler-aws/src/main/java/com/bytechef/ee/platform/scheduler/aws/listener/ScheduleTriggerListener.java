/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Date;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class ScheduleTriggerListener {

    private final ApplicationEventPublisher eventPublisher;

    public ScheduleTriggerListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @SqsListener(AwsTriggerSchedulerConstants.SCHEDULER_SCHEDULE_TRIGGER_QUEUE)
    public void onSchedule(String message) {
        Date fireTime = new Date();
        String[] split = message.split(AwsTriggerSchedulerConstants.SPLITTER);

        eventPublisher.publishEvent(
            new TriggerListenerEvent(
                new TriggerListenerEvent.ListenerParameters(
                    WorkflowExecutionId.parse(split[0]), fireTime.toInstant(),
                    MapUtils.concat(
                        Map.of("datetime", fireTime.toString()), JsonUtils.readMap(split[1], String.class)))));
    }
}
