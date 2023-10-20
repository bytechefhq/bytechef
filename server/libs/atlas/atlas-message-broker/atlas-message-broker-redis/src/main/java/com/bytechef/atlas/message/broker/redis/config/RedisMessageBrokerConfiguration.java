
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "message-broker.provider", havingValue = "redis")
public class RedisMessageBrokerConfiguration implements SmartInitializingSingleton, DisposableBean,
    MessageBrokerListenerRegistrar<RedisMessageBrokerConfiguration.RedisListenerEndpointRegistrar> {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final List<MessageBrokerConfigurer> messageBrokerConfigurers;
    private RedisListenerEndpointRegistrar redisListenerEndpointRegistrar;
    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisMessageBrokerConfiguration(
        @Autowired(required = false) List<MessageBrokerConfigurer> messageBrokerConfigurers,
        RedisTemplate<String, Object> redisTemplate) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null
            ? Collections.emptyList()
            : messageBrokerConfigurers;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterSingletonsInstantiated() {
        redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(redisTemplate);

        for (MessageBrokerConfigurer messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(redisListenerEndpointRegistrar, this);
        }
    }

    @Override
    public void destroy() {
        redisListenerEndpointRegistrar.stop();

        executorService.shutdown();
    }

    @Bean
    RedisMessageBroker redisMessageBroker(RedisTemplate<String, Object> redisTemplate) {
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
        RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

            Jackson2JsonRedisSerializer<Message> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(
                objectMapper, Message.class);

            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.setKeySerializer(RedisSerializer.string());
            redisTemplate.setValueSerializer(new MessageRedisSerializer(jackson2JsonRedisSerializer, objectMapper));

            return redisTemplate;
        }
    }

    private record MessageRedisSerializer(
        Jackson2JsonRedisSerializer<Message> jackson2JsonRedisSerializer, ObjectMapper objectMapper)
        implements RedisSerializer<Object> {

        @Override
        public byte[] serialize(Object message) throws SerializationException {
            Assert.notNull(message, "'message' must not be null.");

            Class<?> messageClass = message.getClass();

            try {
                return jackson2JsonRedisSerializer.serialize(
                    new Message(objectMapper.writeValueAsString(message), messageClass.getName()));
            } catch (JsonProcessingException e) {
                throw new SerializationException(e.getMessage());
            }
        }

        @Override
        @SuppressFBWarnings("NP")
        public Object deserialize(byte[] bytes) throws SerializationException {
            Object object = null;

            if (bytes != null) {
                Message message = Objects.requireNonNull(jackson2JsonRedisSerializer.deserialize(bytes));

                try {
                    object = objectMapper.readValue(message.payload, Class.forName(message.type));
                } catch (Exception e) {
                    throw new SerializationException(e.getMessage());
                }
            }

            return object;
        }
    }

    static class RedisListenerEndpointRegistrar {

        private static final Logger logger = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

        private final RedisTemplate<String, Object> redisTemplate;
        private boolean stopped;

        RedisListenerEndpointRegistrar(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        public void registerListenerEndpoint(String queueName, int concurrency, Object delegate, String methodName) {
            ListOperations<String, Object> listOperations = redisTemplate.opsForList();

            executorService.submit(() -> execute(queueName, delegate, methodName, listOperations));
        }

        private void execute(
            String queueName, Object delegate, String methodName, ListOperations<String, Object> listOperations) {

            while (!stopped) {
                try {
                    Object message = listOperations.rightPop(queueName, Duration.ofSeconds(1));

                    if (message != null) {
                        Method method = getMethod(delegate, methodName, message.getClass());

                        method.invoke(delegate, message);
                    }
                } catch (Exception e) {
                    if (!stopped) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }

        private Method getMethod(Object delegate, String methodName, Class<?> messageClass) {
            Method method = null;

            Class<?> delegateClass = delegate.getClass();

            for (Method curMethod : delegateClass.getDeclaredMethods()) {
                if (Objects.equals(curMethod.getName(), methodName)) {
                    Class<?>[] parameterTypes = curMethod.getParameterTypes();

                    method = curMethod;

                    if (messageClass.equals(parameterTypes[0])) {
                        break;
                    }
                }
            }

            if (method == null) {
                throw new RuntimeException(
                    "Method %s does not exist in class %s".formatted(methodName, delegateClass));
            }

            return method;
        }

        public void stop() {
            this.stopped = true;
        }
    }

    private record Message(String payload, String type) {
    }
}
