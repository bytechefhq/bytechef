/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.message.broker.memory;

import com.bytechef.message.route.MessageRoute;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.util.Assert;

/**
 * Simple, non-thread-safe implementation of the {@link MessageBroker} interface. Useful for testing.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jul 10, 2016
 */
public class SyncMessageBroker extends AbstractMessageBroker {

    @Override
    public void send(MessageRoute messageRoute, Object message) {
        Assert.notNull(messageRoute, "'messageRoute' must not be null");

        List<Receiver> receivers = receiverMap.get(messageRoute);

        Assert.isTrue(receivers != null && !receivers.isEmpty(), "no listeners subscribed for: " + messageRoute);

        for (Receiver receiver : Validate.notNull(receivers, "receivers")) {
            receiver.receive(message);
        }
    }
}
