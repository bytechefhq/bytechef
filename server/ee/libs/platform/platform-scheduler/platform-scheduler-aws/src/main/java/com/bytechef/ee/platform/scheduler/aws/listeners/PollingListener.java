package com.bytechef.ee.platform.scheduler.aws.listeners;

import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.context.ApplicationEventPublisher;

public class PollingListener {
    private ApplicationEventPublisher eventPublisher;

    public PollingListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @SqsListener("polling-queue")
    public void onSchedule(String message) {
        eventPublisher.publishEvent(
            new TriggerPollEvent(WorkflowExecutionId.parse(message)));
    }
}
