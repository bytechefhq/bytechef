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

package com.bytechef.message.broker.redis.env;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerRedis;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.redis.listener.RedisListenerEndpointRegistrar;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerRedis
public class RedisMessageBrokerListenerRegistrarConfiguration implements SmartInitializingSingleton, DisposableBean,
    MessageBrokerListenerRegistrar<RedisListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(
        RedisMessageBrokerListenerRegistrarConfiguration.class);

    private final List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers;
    private MessageListenerAdapter messageListenerAdapter;
    private final RedisConnectionFactory redisConnectionFactory;
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;
    private RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisMessageDeserializer redisMessageDeserializer;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskExecutor taskExecutor;

    @SuppressFBWarnings("EI2")
    public RedisMessageBrokerListenerRegistrarConfiguration(
        @Autowired(
            required = false) List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers,
        RedisConnectionFactory redisConnectionFactory, RedisMessageDeserializer redisMessageDeserializer,
        StringRedisTemplate stringRedisTemplate, TaskExecutor taskExecutor) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null
            ? Collections.emptyList() : messageBrokerConfigurers;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisMessageDeserializer = redisMessageDeserializer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterSingletonsInstantiated() {
        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            redisMessageDeserializer, stringRedisTemplate, taskExecutor);

        redisMessageListenerContainer = new RedisMessageListenerContainer();
        messageListenerAdapter = new MessageListenerAdapter(redisListenerEndpointRegistrar);

        messageListenerAdapter.afterPropertiesSet();

        for (MessageBrokerConfigurer<RedisListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(redisListenerEndpointRegistrar, this);
        }

        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);

        redisListenerEndpointRegistrar.start();
        redisMessageListenerContainer.afterPropertiesSet();
        redisMessageListenerContainer.start();
    }

    @Override
    public void destroy() {
        redisListenerEndpointRegistrar.stop();
        redisMessageListenerContainer.stop();
    }

    @Override
    public void registerListenerEndpoint(
        RedisListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        if (logger.isTraceEnabled()) {
            logger.trace("Registering Redis Listener: {} -> {}:{}", messageRoute, delegateClass, methodName);
        }

        if (messageRoute.isControlExchange()) {
            redisMessageListenerContainer.addMessageListener(
                messageListenerAdapter, new ChannelTopic(messageRoute.getName()));
        } else {
            listenerEndpointRegistrar.registerListenerEndpoint(messageRoute, delegate, methodName);
        }
    }
}
