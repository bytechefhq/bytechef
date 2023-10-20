
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.message.broker.redis.config;

import com.bytechef.message.broker.MessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.redis.listener.RedisListenerEndpointRegistrar;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.oblac.jrsmq.RedisSMQ;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "redis")
public class RedisMessageBrokerListenerRegistrarConfiguration implements SmartInitializingSingleton, DisposableBean,
    MessageBrokerListenerRegistrar<RedisListenerEndpointRegistrar> {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final ChannelTopic controlChannelTopic;
    private final List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final RedisConnectionFactory redisConnectionFactory;
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;
    private RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisMessageDeserializer redisMessageDeserializer;
    private final RedisSMQ redisSMQ;

    @SuppressFBWarnings("EI2")
    public RedisMessageBrokerListenerRegistrarConfiguration(
        @Qualifier("controlChannelTopic") ChannelTopic controlChannelTopic,
        @Autowired(
            required = false) List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers,
        RedisConnectionFactory redisConnectionFactory, RedisMessageDeserializer redisMessageDeserializer,
        RedisSMQ redisSMQ) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null
            ? Collections.emptyList()
            : messageBrokerConfigurers;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisMessageDeserializer = redisMessageDeserializer;
        this.redisSMQ = redisSMQ;
        this.controlChannelTopic = controlChannelTopic;
    }

    @Override
    public void afterSingletonsInstantiated() {
        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            executorService, redisMessageDeserializer, redisSMQ);

        for (MessageBrokerConfigurer<RedisListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(redisListenerEndpointRegistrar, this);
        }

        redisMessageListenerContainer = new RedisMessageListenerContainer();

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisListenerEndpointRegistrar);

        messageListenerAdapter.afterPropertiesSet();

        redisMessageListenerContainer.addMessageListener(messageListenerAdapter, controlChannelTopic);
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);

        redisMessageListenerContainer.afterPropertiesSet();
        redisMessageListenerContainer.start();
    }

    @Override
    public void destroy() throws InterruptedException {
        redisListenerEndpointRegistrar.stop();
        redisMessageListenerContainer.stop();

        executorService.shutdownNow();

        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Override
    public void registerListenerEndpoint(
        RedisListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        listenerEndpointRegistrar.registerListenerEndpoint(messageRoute, delegate, methodName);
    }
}
