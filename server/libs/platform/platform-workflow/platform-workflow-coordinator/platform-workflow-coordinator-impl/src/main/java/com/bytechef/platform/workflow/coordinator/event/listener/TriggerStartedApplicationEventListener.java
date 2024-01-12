/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.coordinator.event.listener;

import com.bytechef.platform.configuration.domain.CancelControlTrigger;
import com.bytechef.platform.workflow.coordinator.event.ApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerStartedApplicationEvent;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.worker.trigger.event.CancelControlTriggerEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerStartedApplicationEventListener implements ApplicationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TriggerStartedApplicationEventListener.class);

    private final ApplicationEventPublisher eventPublisher;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI2")
    public TriggerStartedApplicationEventListener(
        ApplicationEventPublisher eventPublisher, TriggerExecutionService triggerExecutionService) {

        this.eventPublisher = eventPublisher;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof TriggerStartedApplicationEvent triggerStartedApplicationEvent) {
            long triggerExecutionId = triggerStartedApplicationEvent.getTriggerExecutionId();

            TriggerExecution triggerExecution = triggerExecutionService.getTriggerExecution(triggerExecutionId);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Trigger id={}, name='{}', type='{}' started", triggerExecution.getId(), triggerExecution.getName(),
                    triggerExecution.getType());
            }

            if (triggerExecution.getStatus() == Status.CANCELLED) {
                eventPublisher.publishEvent(
                    new CancelControlTriggerEvent(new CancelControlTrigger(triggerExecution.getId())));
            } else {
                if (triggerExecution.getStartDate() == null && triggerExecution.getStatus() != Status.STARTED) {
                    triggerExecution.setStartDate(triggerStartedApplicationEvent.getCreateDate());
                    triggerExecution.setStatus(Status.STARTED);

                    triggerExecutionService.update(triggerExecution);
                }
            }
        }
    }
}
