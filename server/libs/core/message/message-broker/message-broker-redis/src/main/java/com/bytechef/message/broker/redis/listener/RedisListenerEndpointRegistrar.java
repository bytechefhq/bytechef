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

package com.bytechef.message.broker.redis.listener;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.MethodInvoker;

/**
 * @author Ivica Cardic
 */
public class RedisListenerEndpointRegistrar implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

    private static final String CONSUMER_GROUP = "message_event_group";

    private final Map<String, Consumer<String>> invokerMap = new HashMap<>();
    private final RedisMessageDeserializer redisMessageDeserializer;
    private boolean stopped;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskExecutor taskExecutor;

    @SuppressFBWarnings("EI2")
    public RedisListenerEndpointRegistrar(
        RedisMessageDeserializer redisMessageDeserializer, StringRedisTemplate stringRedisTemplate,
        TaskExecutor taskExecutor) {

        this.taskExecutor = taskExecutor;
        this.redisMessageDeserializer = redisMessageDeserializer;
        this.stringRedisTemplate = stringRedisTemplate;
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

        try {
            stringRedisTemplate.opsForStream()
                .createGroup(messageRoute.getName(), CONSUMER_GROUP);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Consumer group already exists or error occurred: {}", e.getMessage());
            }
        }

//        if (messageRoute.isMessageExchange()) {
//            StreamMessageListenerContainer.create(redisConnectionFactory)
//                .receive(
//                    org.springframework.data.redis.connection.stream.Consumer.from(queueName, this.toString()),
//                    StreamOffset.create(queueName, ReadOffset.lastConsumed()),
//                    message -> {
//                        Consumer<String> invokerConsumer = invokerMap.get(queueName);
//
//                        taskExecutor.execute(() -> invokerConsumer.accept(message.getValue().get("message")));
//                    });
//        }
    }

    public void start() {
        this.stopped = false;
        taskExecutor.execute(this::periodicallyCheckQueueForMessage);
    }

    public void stop() {
        this.stopped = true;
    }

    private void periodicallyCheckQueueForMessage() {
        while (!stopped) {
            try {
                for (Map.Entry<String, Consumer<String>> entry : invokerMap.entrySet()) {
                    StreamOperations<String, Object, Object> stringObjectObjectStreamOperations =
                        stringRedisTemplate.opsForStream();

                    List<MapRecord<String, Object, Object>> messages = stringObjectObjectStreamOperations.read(
                        org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, this.toString()),
                        StreamReadOptions.empty(), StreamOffset.create(entry.getKey(), ReadOffset.lastConsumed()));

                    if (messages != null && !messages.isEmpty()) {
                        Consumer<String> invokerConsumer = invokerMap.get(entry.getKey());

                        for (MapRecord<String, Object, Object> message : messages) {
                            Map<Object, Object> value = message.getValue();

                            invokerConsumer.accept((String) value.get("message"));

                            stringObjectObjectStreamOperations.acknowledge(
                                entry.getKey(), CONSUMER_GROUP, message.getId());
                        }
                    }
                }

                sleep();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
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
