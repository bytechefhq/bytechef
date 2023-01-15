
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

import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.atlas.message.broker.redis.RedisMessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "message-broker.provider", havingValue = "redis")
public class RedisMessageBrokerConfiguration implements SmartInitializingSingleton, DisposableBean,
    MessageBrokerListenerRegistrar<RedisListenerEndpointRegistrar> {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final List<MessageBrokerConfigurer> messageBrokerConfigurers;
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;
    private final RedisTemplate<String, RedisMessage> redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisMessageBrokerConfiguration(
        @Autowired(required = false) List<MessageBrokerConfigurer> messageBrokerConfigurers,
        RedisTemplate<String, RedisMessage> redisTemplate) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null
            ? Collections.emptyList()
            : messageBrokerConfigurers;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterSingletonsInstantiated() {
        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(executorService, redisTemplate);

        for (MessageBrokerConfigurer messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(redisListenerEndpointRegistrar, this);
        }
    }

    @Override
    public void destroy() throws InterruptedException {
        redisListenerEndpointRegistrar.stop();

        executorService.shutdownNow();

        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Bean
    RedisMessageBroker redisMessageBroker(RedisTemplate<String, RedisMessage> redisTemplate) {
        return new RedisMessageBroker(redisTemplate);
    }

    @Override
    public void registerListenerEndpoint(
        RedisListenerEndpointRegistrar listenerEndpointRegistrar, String queueName, int concurrency, Object delegate,
        String methodName) {

        listenerEndpointRegistrar.registerListenerEndpoint(queueName, concurrency, delegate, methodName);
    }

    @Configuration
    static class RedisTemplateConfiguration {

        private final ObjectMapper objectMapper;

        RedisTemplateConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Bean
        RedisTemplate<String, RedisMessage> redisMessageRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, RedisMessage> redisTemplate = new RedisTemplate<>();

            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.setKeySerializer(RedisSerializer.string());
            redisTemplate.setValueSerializer(new RedisMessageRedisSerializer(objectMapper));

            return redisTemplate;
        }
    }
}
