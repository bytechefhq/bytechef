package com.bytechef.ee.platform.scheduler.aws.listeners;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Date;
import java.util.Map;

public class ScheduleListener {
    private ApplicationEventPublisher eventPublisher;

    public ScheduleListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @SqsListener("schedule-queue")
    public void onSchedule(String message) {
        Date fireTime = new Date();
        String[] split = message.split("\\|_\\$plitter_\\|");

        eventPublisher.publishEvent(
            new TriggerListenerEvent(
                new TriggerListenerEvent.ListenerParameters(
                    WorkflowExecutionId.parse(split[1]),
                    LocalDateTimeUtils.toLocalDateTime(fireTime),
                    MapUtils.concat(
                        Map.of("datetime", fireTime.toString()),
                        JsonUtils.readMap(split[0], String.class)))));
    }
}
