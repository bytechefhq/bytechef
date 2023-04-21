
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.message.broker.jms.config;

import com.bytechef.message.broker.ExchangeType;
import com.bytechef.message.broker.Queues;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.jms.JmsMessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "jms")
public class JmsMessageBrokerConfiguration
    implements JmsListenerConfigurer, MessageBrokerListenerRegistrar<JmsListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageBrokerConfiguration.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private List<MessageBrokerConfigurer<JmsListenerEndpointRegistrar>> messageBrokerConfigurers = Collections
        .emptyList();

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<JmsListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        JmsListenerEndpointRegistrar listenerEndpointRegistrar, String queueName, int concurrency, Object delegate,
        String methodName) {

        if (Objects.equals(queueName, Queues.TASKS_CONTROL)) {
            queueName = ExchangeType.CONTROL + "/" + ExchangeType.CONTROL;
        }

        logger.info("Registering JMS Listener: {} -> {}:{}", queueName, delegate.getClass(), methodName);

        MessageListenerAdapter messageListenerAdapter = new NoReplyMessageListenerAdapter(delegate);

        messageListenerAdapter.setMessageConverter(jacksonJmsMessageConverter(objectMapper));
        messageListenerAdapter.setDefaultListenerMethod(methodName);

        SimpleJmsListenerEndpoint simpleJmsListenerEndpoint = new SimpleJmsListenerEndpoint();

        simpleJmsListenerEndpoint.setId(queueName + "Endpoint");
        simpleJmsListenerEndpoint.setDestination(queueName);
        simpleJmsListenerEndpoint.setMessageListener(messageListenerAdapter);

        listenerEndpointRegistrar.registerEndpoint(simpleJmsListenerEndpoint, createContainerFactory(concurrency));
    }

    @Bean
    MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();

        mappingJackson2MessageConverter.setObjectMapper(objectMapper);
        mappingJackson2MessageConverter.setTargetType(MessageType.TEXT);
        mappingJackson2MessageConverter.setTypeIdPropertyName("_type");

        return mappingJackson2MessageConverter;
    }

    @Bean
    JmsListenerContainerFactory<?> jmsListenerContainerFactory(
        ConnectionFactory connectionFactory,
        DefaultJmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer) {

        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();

        jmsListenerContainerFactoryConfigurer.configure(jmsListenerContainerFactory, connectionFactory);

        return jmsListenerContainerFactory;
    }

    @Bean
    JmsMessageBroker jmsMessageBroker(JmsTemplate jmsTemplate) {
        return new JmsMessageBroker(jmsTemplate);
    }

    private DefaultJmsListenerContainerFactory createContainerFactory(int concurrency) {
        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();

        jmsListenerContainerFactory.setConcurrency(String.valueOf(concurrency));
        jmsListenerContainerFactory.setConnectionFactory(connectionFactory);

        return jmsListenerContainerFactory;
    }

    private static class NoReplyMessageListenerAdapter extends MessageListenerAdapter {

        public NoReplyMessageListenerAdapter(Object delegate) {
            super(delegate);
        }

        @Override
        protected void handleResult(Object result, Message request, Session session) {
            // ignore
        }
    }
}
