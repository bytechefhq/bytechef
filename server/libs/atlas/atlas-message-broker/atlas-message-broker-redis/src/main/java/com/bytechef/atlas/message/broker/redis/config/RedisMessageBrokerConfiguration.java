
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

package com.bytechef.atlas.message.broker.redis.config;

import com.bytechef.atlas.message.broker.WorkflowExchange;
import com.bytechef.atlas.message.broker.TaskQueues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.atlas.message.broker.redis.listener.RedisListenerEndpointRegistrar;
import com.bytechef.atlas.message.broker.redis.RedisMessageBroker;
import com.bytechef.atlas.message.broker.redis.serializer.RedisMessageSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oblac.jrsmq.RedisSMQ;
import com.oblac.jrsmq.RedisSMQConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "redis")
public class RedisMessageBrokerConfiguration implements SmartInitializingSingleton, DisposableBean,
    MessageBrokerListenerRegistrar<RedisListenerEndpointRegistrar> {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final RedisConnectionFactory redisConnectionFactory;
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;
    private RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisMessageSerializer redisMessageSerializer;
    private final RedisSMQ redisSMQ;

    @SuppressFBWarnings("EI2")
    public RedisMessageBrokerConfiguration(
        @Autowired(
            required = false) List<MessageBrokerConfigurer<RedisListenerEndpointRegistrar>> messageBrokerConfigurers,
        RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper,
        RedisProperties redisProperties) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null
            ? Collections.emptyList()
            : messageBrokerConfigurers;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisMessageSerializer = new RedisMessageSerializer(objectMapper);
        this.redisSMQ = new RedisSMQ(
            RedisSMQConfig.createDefaultConfig()
                .database(redisProperties.getDatabase())
                .host(redisProperties.getHost())
                .password(redisProperties.getPassword())
                .port(redisProperties.getPort())
                .ssl(redisProperties.isSsl())
                .timeout(getTimeout(redisProperties.getTimeout())));
    }

    @Override
    public void afterSingletonsInstantiated() {
        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            executorService, redisMessageSerializer, redisSMQ);

        for (MessageBrokerConfigurer<RedisListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(redisListenerEndpointRegistrar, this);
        }

        redisMessageListenerContainer = new RedisMessageListenerContainer();

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisListenerEndpointRegistrar);

        messageListenerAdapter.afterPropertiesSet();

        redisMessageListenerContainer.addMessageListener(messageListenerAdapter, channelTopic());
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

    @Bean
    ChannelTopic channelTopic() {
        return new ChannelTopic(WorkflowExchange.CONTROL + "/" + WorkflowExchange.CONTROL);
    }

    @Bean
    RedisMessageBroker redisMessageBroker(StringRedisTemplate stringRedisTemplate) {
        return new RedisMessageBroker(redisMessageSerializer, redisSMQ, stringRedisTemplate);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Override
    public void registerListenerEndpoint(
        RedisListenerEndpointRegistrar listenerEndpointRegistrar, String queueName, int concurrency, Object delegate,
        String methodName) {

        WorkflowExchange workflowExchange = WorkflowExchange.TASKS;

        if (Objects.equals(queueName, TaskQueues.CONTROL)) {
            workflowExchange = WorkflowExchange.CONTROL;
            queueName = WorkflowExchange.CONTROL + "/" + WorkflowExchange.CONTROL;
        }

        listenerEndpointRegistrar.registerListenerEndpoint(queueName, delegate, methodName, workflowExchange);
    }

    private static int getTimeout(Duration timeout) {
        return timeout == null ? 5000 : (int) timeout.toMillis();
    }
}
