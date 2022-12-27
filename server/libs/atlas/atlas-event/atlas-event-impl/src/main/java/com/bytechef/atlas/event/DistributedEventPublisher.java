
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.event;

import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
public class DistributedEventPublisher implements EventPublisher {

    private final MessageBroker messageBroker;

    public DistributedEventPublisher(MessageBroker messageBroker) {
        this.messageBroker = Objects.requireNonNull(messageBroker);
    }

    @Override
    public void publishEvent(WorkflowEvent workflowEvent) {
        messageBroker.send(Queues.EVENTS, workflowEvent);
    }
}
