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

package com.bytechef.message.event.listener;

import static com.bytechef.tenant.TenantContext.CURRENT_TENANT_ID;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPreSendProcessor;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
    private final List<MessageEventPreSendProcessor> messageEventPreSendProcessors;

    @SuppressFBWarnings("EI")
    public MessageEventListener(
        MessageBroker messageBroker, List<MessageEventPreSendProcessor> messageEventPreSendProcessors) {

        this.messageBroker = messageBroker;
        this.messageEventPreSendProcessors = messageEventPreSendProcessors;
    }

    @EventListener
    @Async
    public void onMessageEvent(MessageEvent<?> messageEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onMessageEvent: " + messageEvent);
        }

        for (MessageEventPreSendProcessor messageEventPreSendProcessor : messageEventPreSendProcessors) {
            messageEvent = messageEventPreSendProcessor.process(messageEvent);
        }

        messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        messageBroker.send(messageEvent.getRoute(), messageEvent);
    }
}
