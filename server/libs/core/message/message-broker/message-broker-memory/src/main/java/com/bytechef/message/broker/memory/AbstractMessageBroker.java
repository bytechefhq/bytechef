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

package com.bytechef.message.broker.memory;

import com.bytechef.message.route.MessageRoute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base implementation of the {@link MessageBroker} interface. This class provides a framework for managing
 * receivers and routing messages to them based on specified routes. Concrete subclasses need to implement the
 * {@code send} method to define the specific behavior for message delivery.
 *
 * @author Ivica Cardic
 */
public abstract class AbstractMessageBroker implements MemoryMessageBroker {

    protected final Map<MessageRoute, List<Receiver>> receiverMap = new HashMap<>();

    @Override
    public void receive(MessageRoute messageRoute, Receiver receiver) {
        List<Receiver> receivers = receiverMap.computeIfAbsent(messageRoute, k -> new ArrayList<>());

        receivers.add(receiver);
    }
}
