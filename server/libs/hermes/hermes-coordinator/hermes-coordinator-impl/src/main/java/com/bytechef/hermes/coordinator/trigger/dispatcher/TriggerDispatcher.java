
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

package com.bytechef.hermes.coordinator.trigger.dispatcher;

import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TriggerDispatcher.class);

    private final MessageBroker messageBroker;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;

    @SuppressFBWarnings("EI")
    public TriggerDispatcher(
        MessageBroker messageBroker, List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors) {

        this.messageBroker = messageBroker;
        this.triggerDispatcherPreSendProcessors = triggerDispatcherPreSendProcessors;
    }

    public void dispatch(TriggerExecution triggerExecution) {
        triggerExecution = preProcess(triggerExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger id={}, type='{}' sent to route='{}'", triggerExecution.getId(), triggerExecution.getType(),
                TriggerMessageRoute.TRIGGERS);
        }

        messageBroker.send(TriggerMessageRoute.TRIGGERS, triggerExecution);
    }

    private TriggerExecution preProcess(TriggerExecution triggerExecution) {
        for (TriggerDispatcherPreSendProcessor triggerDispatcherPreSendProcessor : triggerDispatcherPreSendProcessors) {
            triggerExecution = triggerDispatcherPreSendProcessor.process(triggerExecution);
        }

        return triggerExecution;
    }
}
