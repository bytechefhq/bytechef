/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.message.broker.kafka.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerKafka;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnMessageBrokerKafka
public class KafkaMessageBrokerListenerRegistrarConfiguration
    implements KafkaListenerConfigurer, MessageBrokerListenerRegistrar<KafkaListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(
        KafkaMessageBrokerListenerRegistrarConfiguration.class);

    private final BeanFactory beanFactory;
    private final List<MessageBrokerConfigurer<KafkaListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final MessageHandlerMethodFactory messageHandlerMethodFactory;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @SuppressFBWarnings("EI")
    public KafkaMessageBrokerListenerRegistrarConfiguration(
        BeanFactory beanFactory,
        @Autowired(
            required = false) List<MessageBrokerConfigurer<KafkaListenerEndpointRegistrar>> messageBrokerConfigurers,
        MessageHandlerMethodFactory messageHandlerMethodFactory,
        @Autowired(required = false) KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {

        this.beanFactory = beanFactory;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.messageHandlerMethodFactory = messageHandlerMethodFactory;
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<KafkaListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        KafkaListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        if (logger.isTraceEnabled()) {
            logger.trace("Registering Kafka Listener: {} -> {}:{}", messageRoute, delegateClass.getName(), methodName);
        }

        Method listenerMethod = Stream.of(delegateClass.getMethods())
            .filter(it -> methodName.equals(it.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No method found: " + methodName + " on " + delegate.getClass()));

        MethodKafkaListenerEndpoint<String, String> endpoint = createListenerEndpoint(
            messageRoute.getName(), delegate, listenerMethod);

        listenerEndpointRegistrar.registerEndpoint(endpoint);
    }

    @Override
    public void stopListenerEndpoints() {
        try {
            if (kafkaListenerEndpointRegistry != null) {
                kafkaListenerEndpointRegistry.stop();
            }
        } catch (Exception e) {
            logger.warn("Failed to stop Kafka listener containers: {}", e.getMessage());
        }
    }

    @Override
    public void startListenerEndpoints() {
        try {
            if (kafkaListenerEndpointRegistry != null) {
                kafkaListenerEndpointRegistry.start();
            }
        } catch (Exception e) {
            logger.warn("Failed to start Kafka listener containers: {}", e.getMessage());
        }
    }

    private MethodKafkaListenerEndpoint<String, String> createListenerEndpoint(
        String queueName, Object listener, Method listenerMethod) {

        final MethodKafkaListenerEndpoint<String, String> endpoint = new MethodKafkaListenerEndpoint<>();

        endpoint.setBeanFactory(beanFactory);
        endpoint.setBean(listener);
        endpoint.setMethod(listenerMethod);
        endpoint.setId(queueName + "Endpoint");
        endpoint.setTopics(queueName);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);

        return endpoint;
    }
}
