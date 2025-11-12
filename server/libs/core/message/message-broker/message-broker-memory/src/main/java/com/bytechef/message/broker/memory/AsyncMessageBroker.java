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

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.message.Retryable;
import com.bytechef.message.route.MessageRoute;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * An asynchronous implementation of a message broker for routing messages to subscribed listeners. This class extends
 * {@code AbstractMessageBroker}, providing a non-blocking approach to message delivery using an {@code Executor} for
 * task execution. The {@code AsyncMessageBroker} is designed to decouple the process of message sending and delivery by
 * executing receiver logic in separate threads managed by the provided executor.
 *
 * @author Ivica Cardic
 */
public class AsyncMessageBroker extends AbstractMessageBroker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageBroker.class);

    private final Executor executor;

    public AsyncMessageBroker(Environment environment) {
        if (Threading.VIRTUAL.isActive(environment)) {
            executor = Executors.newVirtualThreadPerTaskExecutor();
        } else {
            executor = Executors.newCachedThreadPool();
        }
    }

    @Override
    public void send(MessageRoute messageRoute, Object message) {
        Assert.notNull(messageRoute, "'messageRoute' must not be null");

        if (message instanceof Retryable retryable) {
            delay(retryable.getRetryDelayMillis());
        }

        List<Receiver> receivers = receiverMap.get(messageRoute);

        Assert.isTrue(receivers != null && !receivers.isEmpty(), "no listeners subscribed for: " + messageRoute);

        for (Receiver receiver : Validate.notNull(receivers, "receivers")) {
            executor.execute(() -> receiver.receive(
                ConvertUtils.convertValue(JsonUtils.read(JsonUtils.write(message)), message.getClass())));
        }
    }

    private void delay(long value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }
    }
}
