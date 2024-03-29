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

package com.bytechef.message.event.listener;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class MessageEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageEventListener.class);

    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI")
    public MessageEventListener(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @EventListener
    @Async
    public void onMessageEvent(MessageEvent<?> messageEvent) {
        if (logger.isDebugEnabled()) {
            logger.debug("onMessageEvent: " + messageEvent);
        }

        messageBroker.send(messageEvent.getRoute(), messageEvent);
    }
}
