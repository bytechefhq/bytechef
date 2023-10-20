
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

import com.bytechef.event.WorkflowEvent;
import com.bytechef.event.listener.EventListener;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.domain.TriggerExecution.Status;
import com.bytechef.hermes.event.TriggerStartedWorkflowEvent;
import com.bytechef.hermes.service.TriggerExecutionService;
import com.bytechef.hermes.trigger.CancelControlTrigger;
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

    private static final Logger log = LoggerFactory.getLogger(TriggerStartedEventListener.class);

    private final MessageBroker messageBroker;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI2")
    public TriggerStartedEventListener(MessageBroker messageBroker, TriggerExecutionService triggerExecutionService) {
        this.messageBroker = messageBroker;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (TriggerStartedWorkflowEvent.TRIGGER_STARTED.equals(workflowEvent.getType())) {
            long triggerExecutionId =
                ((TriggerStartedWorkflowEvent) workflowEvent).getTriggerExecutionId();

            TriggerExecution triggerExecution = triggerExecutionService.getTriggerExecution(triggerExecutionId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "Trigger id={}, name='{}', type='{}' started", triggerExecution.getId(), triggerExecution.getName(),
                    triggerExecution.getType());
            }

            if (triggerExecution.getStatus() == Status.CANCELLED) {
                messageBroker.send(SystemMessageRoute.CONTROL, new CancelControlTrigger(triggerExecution.getId()));
            } else {
                if (triggerExecution.getStartDate() == null && triggerExecution.getStatus() != Status.STARTED) {
                    triggerExecution.setStartDate(workflowEvent.getCreatedDate());
                    triggerExecution.setStatus(Status.STARTED);

                    triggerExecutionService.update(triggerExecution);
                }
            }
        }
    }
}
