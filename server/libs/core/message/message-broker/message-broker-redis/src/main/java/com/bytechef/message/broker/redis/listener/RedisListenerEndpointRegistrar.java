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
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
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

    /**
     * Minimum idle time before a pending entry left by another (crashed) consumer is reclaimed. Long enough that a
     * healthy consumer's in-flight message is never stolen, short enough that redelivery after a crash is prompt.
     */
    private static final Duration RECLAIM_MIN_IDLE = Duration.ofSeconds(60);

    private static final long RECLAIM_INTERVAL_MILLIS = 10_000;
    private static final int RECLAIM_BATCH_SIZE = 100;

    private final Map<String, Consumer<String>> invokerMap = new HashMap<>();
    private long lastReclaimTimeMillis;
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
            log.warn("No message listeners registered for queue='{}'", queueName);

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
            if (log.isDebugEnabled()) {
                log.debug("Consumer group already exists or error occurred: {}", e.getMessage());
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

                long currentTimeMillis = System.currentTimeMillis();

                if (currentTimeMillis - lastReclaimTimeMillis >= RECLAIM_INTERVAL_MILLIS) {
                    lastReclaimTimeMillis = currentTimeMillis;

                    reclaimAbandonedPendingMessages();
                }

                sleep();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Redelivers stream entries that were read but never acknowledged by a consumer that has since died (XPENDING +
     * XCLAIM). Consumer names are per-instance, so a crashed JVM's pending entries would otherwise sit in the consumer
     * group's pending list forever — {@code lastConsumed} reads only deliver NEW entries. Claiming uses the same
     * min-idle threshold as the pending filter, so an entry another live consumer touched in the meantime is skipped
     * atomically. Processing a reclaimed entry follows the normal path (invoke, then acknowledge), giving the redis
     * broker the same at-least-once redelivery semantics as amqp.
     */
    void reclaimAbandonedPendingMessages() {
        for (Map.Entry<String, Consumer<String>> entry : invokerMap.entrySet()) {
            String queueName = entry.getKey();

            StreamOperations<String, Object, Object> streamOperations = stringRedisTemplate.opsForStream();

            PendingMessages pendingMessages = streamOperations.pending(
                queueName, CONSUMER_GROUP, Range.unbounded(), RECLAIM_BATCH_SIZE);

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                continue;
            }

            RecordId[] abandonedRecordIds = pendingMessages.stream()
                .filter(pendingMessage -> {
                    Duration elapsedTimeSinceLastDelivery = pendingMessage.getElapsedTimeSinceLastDelivery();

                    return elapsedTimeSinceLastDelivery.compareTo(RECLAIM_MIN_IDLE) >= 0;
                })
                .map(PendingMessage::getId)
                .toArray(RecordId[]::new);

            if (abandonedRecordIds.length == 0) {
                continue;
            }

            List<MapRecord<String, Object, Object>> claimedMessages = streamOperations.claim(
                queueName, CONSUMER_GROUP, this.toString(), RECLAIM_MIN_IDLE, abandonedRecordIds);

            if (claimedMessages == null || claimedMessages.isEmpty()) {
                continue;
            }

            log.warn(
                "Reclaimed {} abandoned pending message(s) on queue='{}' left by a crashed consumer; redelivering",
                claimedMessages.size(), queueName);

            Consumer<String> invokerConsumer = entry.getValue();

            for (MapRecord<String, Object, Object> claimedMessage : claimedMessages) {
                Map<Object, Object> value = claimedMessage.getValue();

                invokerConsumer.accept((String) value.get("message"));

                streamOperations.acknowledge(queueName, CONSUMER_GROUP, claimedMessage.getId());
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
