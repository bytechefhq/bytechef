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

package com.bytechef.platform.workflow.coordinator.trigger.dispatcher;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.trigger.event.TriggerExecutionEvent;
import com.bytechef.platform.workflow.worker.trigger.message.route.TriggerWorkerMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TriggerDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;

    @SuppressFBWarnings("EI")
    public TriggerDispatcher(
        ApplicationEventPublisher eventPublisher,
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors) {

        this.eventPublisher = eventPublisher;
        this.triggerDispatcherPreSendProcessors = triggerDispatcherPreSendProcessors;
    }

    public void dispatch(TriggerExecution triggerExecution) {
        triggerExecution = preProcess(triggerExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger id={}, type='{}' sent to route='{}'", triggerExecution.getId(), triggerExecution.getType(),
                TriggerWorkerMessageRoute.TRIGGER_EXECUTION_EVENTS);
        }

        eventPublisher.publishEvent(new TriggerExecutionEvent(triggerExecution));
    }

    private TriggerExecution preProcess(TriggerExecution triggerExecution) {
        for (TriggerDispatcherPreSendProcessor triggerDispatcherPreSendProcessor : triggerDispatcherPreSendProcessors) {
            triggerExecution = triggerDispatcherPreSendProcessor.process(triggerExecution);
        }

        return triggerExecution;
    }
}
