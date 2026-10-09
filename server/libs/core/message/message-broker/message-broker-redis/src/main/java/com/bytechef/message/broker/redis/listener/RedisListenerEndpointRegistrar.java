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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static final Logger log = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

    private static final String CONSUMER_GROUP = "message_event_group";

    private final RedisMessageDeserializer redisMessageDeserializer;
    private volatile boolean stopped;
    private final Map<String, AtomicInteger> streamInvokerSequenceMap = new HashMap<>();
    private final Map<String, List<Consumer<String>>> streamInvokersMap = new HashMap<>();
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskExecutor taskExecutor;
    private final Map<String, List<Consumer<String>>> topicInvokersMap = new HashMap<>();

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
        String channelName = new String(message.getChannel(), StandardCharsets.UTF_8);

        List<Consumer<String>> invokers = topicInvokersMap.get(channelName);

        if (invokers == null) {
            log.warn("No message listeners registered for channel='{}'", channelName);

            return;
        }

        dispatch(invokers, new String(message.getBody(), StandardCharsets.UTF_8));
    }

    public void registerListenerEndpoint(MessageRoute messageRoute, Object delegate, String methodName) {
        String routeName = messageRoute.getName();

        Consumer<String> invoker = (String message) -> invoke(delegate, methodName, message);

        if (messageRoute.isControlExchange()) {
            List<Consumer<String>> invokers = topicInvokersMap.computeIfAbsent(routeName, key -> new ArrayList<>());

            invokers.add(invoker);

            return;
        }

        List<Consumer<String>> invokers = streamInvokersMap.computeIfAbsent(routeName, key -> new ArrayList<>());

        invokers.add(invoker);

        try {
            stringRedisTemplate.opsForStream()
                .createGroup(routeName, CONSUMER_GROUP);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Consumer group already exists or error occurred: {}", e.getMessage());
            }
        }
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
                for (Map.Entry<String, List<Consumer<String>>> entry : streamInvokersMap.entrySet()) {
                    StreamOperations<String, Object, Object> stringObjectObjectStreamOperations =
                        stringRedisTemplate.opsForStream();

                    List<MapRecord<String, Object, Object>> messages = stringObjectObjectStreamOperations.read(
                        org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, this.toString()),
                        StreamReadOptions.empty(), StreamOffset.create(entry.getKey(), ReadOffset.lastConsumed()));

                    if (messages != null && !messages.isEmpty()) {
                        for (MapRecord<String, Object, Object> message : messages) {
                            Map<Object, Object> value = message.getValue();

                            Consumer<String> streamInvoker = getNextStreamInvoker(entry.getKey(), entry.getValue());

                            streamInvoker.accept((String) value.get("message"));

                            stringObjectObjectStreamOperations.acknowledge(
                                entry.getKey(), CONSUMER_GROUP, message.getId());
                        }
                    }
                }

                sleep();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void dispatch(List<Consumer<String>> invokers, String message) {
        for (Consumer<String> invoker : invokers) {
            invoker.accept(message);
        }
    }

    private Consumer<String> getNextStreamInvoker(String routeName, List<Consumer<String>> invokers) {
        AtomicInteger streamInvokerSequence = streamInvokerSequenceMap.computeIfAbsent(
            routeName, key -> new AtomicInteger());

        return invokers.get(Math.floorMod(streamInvokerSequence.getAndIncrement(), invokers.size()));
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
                log.error(e.getMessage(), e);
            }
        }
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            if (log.isTraceEnabled()) {
                log.trace(e.getMessage(), e);
            }
        }
    }
}
