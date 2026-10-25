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

package com.bytechef.message.broker.redis.control;

import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.MethodInvoker;

/**
 * Broadcasts CONTROL route messages to every node over Redis pub/sub: each node subscribes to the route's channel and
 * dispatches every message to all delegates registered for it.
 *
 * @author Ivica Cardic
 */
public class RedisControlChannel {

    private static final Logger log = LoggerFactory.getLogger(RedisControlChannel.class);

    private final RedisMessageDeserializer redisMessageDeserializer;
    private final RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
    private volatile boolean stopped;
    private final Map<String, List<Consumer<String>>> topicInvokersMap = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI2")
    public RedisControlChannel(
        RedisConnectionFactory redisConnectionFactory, RedisMessageDeserializer redisMessageDeserializer) {

        this.redisMessageDeserializer = redisMessageDeserializer;

        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.afterPropertiesSet();
    }

    public boolean isRunning() {
        return redisMessageListenerContainer.isRunning();
    }

    public synchronized void register(MessageRoute messageRoute, Object delegate, String methodName) {
        String channelName = messageRoute.getName();

        Consumer<String> invoker = (String message) -> invoke(delegate, methodName, message);

        boolean subscribed = topicInvokersMap.containsKey(channelName);

        List<Consumer<String>> invokers = topicInvokersMap.computeIfAbsent(
            channelName, key -> new CopyOnWriteArrayList<>());

        invokers.add(invoker);

        if (!subscribed) {
            MessageListener messageListener = (Message message, byte[] pattern) -> onMessage(
                new String(message.getChannel(), StandardCharsets.UTF_8),
                new String(message.getBody(), StandardCharsets.UTF_8));

            redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(channelName));
        }
    }

    public void start() {
        stopped = false;

        redisMessageListenerContainer.start();
    }

    public void stop() {
        stopped = true;

        redisMessageListenerContainer.stop();
    }

    Set<String> getSubscribedChannelNames() {
        return Set.copyOf(topicInvokersMap.keySet());
    }

    void onMessage(String channelName, String body) {
        List<Consumer<String>> invokers = topicInvokersMap.get(channelName);

        if (invokers == null) {
            log.warn("No message listeners registered for channel='{}'", channelName);

            return;
        }

        for (Consumer<String> invoker : invokers) {
            invoker.accept(body);
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
}
