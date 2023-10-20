
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

package com.bytechef.atlas.message.broker.jms.config;

import com.bytechef.atlas.message.broker.Exchanges;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.atlas.message.broker.jms.JmsMessageBroker;
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
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "message-broker.provider", havingValue = "jms")
public class JmsMessageBrokerConfiguration
    implements JmsListenerConfigurer, MessageBrokerListenerRegistrar<JmsListenerEndpointRegistrar> {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private List<MessageBrokerConfigurer> messageBrokerConfigurers = Collections.emptyList();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    JmsMessageBroker jmsMessageBroker(JmsTemplate aJmsTemplate) {
        JmsMessageBroker jmsMessageBroker = new JmsMessageBroker();
        jmsMessageBroker.setJmsTemplate(aJmsTemplate);

        return jmsMessageBroker;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        return converter;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
        ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();

        configurer.configure(factory, connectionFactory);

        return factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configureJmsListeners(JmsListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<JmsListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        JmsListenerEndpointRegistrar listenerEndpointRegistrar, String queueName, int concurrency, Object delegate,
        String methodName) {

        if (Objects.equals(queueName, Queues.CONTROL)) {
            queueName = Exchanges.CONTROL + "/" + Exchanges.CONTROL;
        }

        logger.info("Registering JMS Listener: {} -> {}:{}", queueName, delegate.getClass(), methodName);

        MessageListenerAdapter messageListener = new NoReplyMessageListenerAdapter(delegate);

        messageListener.setMessageConverter(jacksonJmsMessageConverter(objectMapper));
        messageListener.setDefaultListenerMethod(methodName);

        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();

        endpoint.setId(queueName + "Endpoint");
        endpoint.setDestination(queueName);
        endpoint.setMessageListener(messageListener);

        listenerEndpointRegistrar.registerEndpoint(endpoint, createContainerFactory(concurrency));
    }

    private DefaultJmsListenerContainerFactory createContainerFactory(int aConcurrency) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();

        factory.setConcurrency(String.valueOf(aConcurrency));
        factory.setConnectionFactory(connectionFactory);

        return factory;
    }

    private static class NoReplyMessageListenerAdapter extends MessageListenerAdapter {

        public NoReplyMessageListenerAdapter(Object aDelegate) {
            super(aDelegate);
        }

        @Override
        protected void handleResult(Object aResult, Message aRequest, Session aSession) {
            // ignore
        }
    }
}
