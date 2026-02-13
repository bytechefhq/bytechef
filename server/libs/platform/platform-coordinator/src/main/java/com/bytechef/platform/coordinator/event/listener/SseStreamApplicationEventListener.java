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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SseStreamApplicationEventListener implements ApplicationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SseStreamApplicationEventListener.class);

    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI")
    public SseStreamApplicationEventListener(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {
            publishJobStatusEvent(jobStatusApplicationEvent);
        } else if (applicationEvent instanceof TaskStartedApplicationEvent taskStartedApplicationEvent) {
            publishTaskStartedEvent(taskStartedApplicationEvent);
        }
    }

    private void publishJobStatusEvent(JobStatusApplicationEvent jobStatusApplicationEvent) {
        try {
            Job.Status status = jobStatusApplicationEvent.getStatus();

            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobStatusApplicationEvent.getJobId(), SseStreamEvent.EVENT_TYPE_JOB_STATUS, status.name());

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }

    private void publishTaskStartedEvent(TaskStartedApplicationEvent taskStartedApplicationEvent) {
        Long jobId = taskStartedApplicationEvent.getJobId();

        if (jobId == null) {
            return;
        }

        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobId, SseStreamEvent.EVENT_TYPE_TASK_STARTED, taskStartedApplicationEvent.getTaskExecutionId());

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }
}
