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

package com.bytechef.message.broker.redis.listener;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import com.oblac.jrsmq.QueueMessage;
import com.oblac.jrsmq.RedisSMQ;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.util.MethodInvoker;

/**
 * @author Ivica Cardic
 */
public class RedisListenerEndpointRegistrar implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

    private final TaskExecutor taskExecutor;
    private final Map<String, Consumer<String>> invokerMap = new HashMap<>();
    private final RedisMessageDeserializer redisMessageDeserializer;
    private final RedisSMQ redisSMQ;
    private boolean stopped;

    @SuppressFBWarnings("EI2")
    public RedisListenerEndpointRegistrar(
        RedisMessageDeserializer redisMessageDeserializer, RedisSMQ redisSMQ, TaskExecutor taskExecutor) {

        this.taskExecutor = taskExecutor;
        this.redisMessageDeserializer = redisMessageDeserializer;
        this.redisSMQ = redisSMQ;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String queueName = new String(message.getChannel(), StandardCharsets.UTF_8);

        Consumer<String> invokerConsumer = invokerMap.get(queueName);

        if (invokerConsumer == null) {
            logger.warn("No message listeners registered for queue='{}'", queueName);

            return;
        }

        invokerConsumer.accept(message.toString());
    }

    public void registerListenerEndpoint(MessageRoute messageRoute, Object delegate, String methodName) {
        String queueName = messageRoute.getName();

        invokerMap.put(queueName, (String message) -> invoke(delegate, methodName, message));

        checkQueueExists(queueName);

        if (messageRoute.isMessageExchange()) {
            taskExecutor.execute(() -> periodicallyCheckQueueForMessage(queueName));
        }
    }

    public void stop() {
        this.stopped = true;
    }

    private void periodicallyCheckQueueForMessage(String queueName) {
        while (!stopped) {
            try {
                QueueMessage queueMessage = redisSMQ.popMessage()
                    .qname(queueName)
                    .exec();

                if (queueMessage == null) {
                    sleep();
                } else {
                    Consumer<String> invokerConsumer = invokerMap.get(queueName);

                    taskExecutor.execute(() -> invokerConsumer.accept(queueMessage.message()));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void checkQueueExists(String queueName) {
        Set<String> queueNames = redisSMQ.listQueues()
            .exec();

        if (!queueNames.contains(queueName)) {
            redisSMQ.createQueue()
                .qname(queueName)
                .exec();
        }
    }

    private void invoke(Object delegate, String methodName, String messageString) {
        try {
            Object message = redisMessageDeserializer.deserialize(messageString);

            MethodInvoker methodInvoker = new MethodInvoker();

            methodInvoker.setTargetObject(delegate);
            methodInvoker.setTargetMethod(methodName);
            methodInvoker.setArguments(message);

            methodInvoker.prepare();

            methodInvoker.invoke();
        } catch (Exception e) {
            if (!stopped) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }
    }
}
