
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.coordinator.event;

import com.bytechef.event.Event;
import com.bytechef.event.listener.EventListener;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.event.TriggerStartedEvent;
import com.bytechef.hermes.execution.service.RemoteTriggerExecutionService;
import com.bytechef.hermes.configuration.trigger.CancelControlTrigger;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerStartedEventListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(TriggerStartedEventListener.class);

    private final MessageBroker messageBroker;
    private final RemoteTriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI2")
    public TriggerStartedEventListener(
        MessageBroker messageBroker, RemoteTriggerExecutionService triggerExecutionService) {

        this.messageBroker = messageBroker;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public void onApplicationEvent(Event event) {
        if (TriggerStartedEvent.TRIGGER_STARTED.equals(event.getType())) {
            long triggerExecutionId =
                ((TriggerStartedEvent) event).getTriggerExecutionId();

            TriggerExecution triggerExecution = triggerExecutionService.getTriggerExecution(triggerExecutionId);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Trigger id={}, name='{}', type='{}' started", triggerExecution.getId(), triggerExecution.getName(),
                    triggerExecution.getType());
            }

            if (triggerExecution.getStatus() == Status.CANCELLED) {
                messageBroker.send(SystemMessageRoute.CONTROL, new CancelControlTrigger(triggerExecution.getId()));
            } else {
                if (triggerExecution.getStartDate() == null && triggerExecution.getStatus() != Status.STARTED) {
                    triggerExecution.setStartDate(event.getCreatedDate());
                    triggerExecution.setStatus(Status.STARTED);

                    triggerExecutionService.update(triggerExecution);
                }
            }
        }
    }
}
