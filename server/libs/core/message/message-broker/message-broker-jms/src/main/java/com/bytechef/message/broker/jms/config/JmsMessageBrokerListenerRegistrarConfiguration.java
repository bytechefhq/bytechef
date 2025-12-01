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

package com.bytechef.message.broker.jms.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerJms;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerJms
public class JmsMessageBrokerListenerRegistrarConfiguration
    implements JmsListenerConfigurer, MessageBrokerListenerRegistrar<JmsListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageBrokerListenerRegistrarConfiguration.class);

    private final ConnectionFactory connectionFactory;
    private final MessageConverter jacksonJmsMessageConverter;
    private final List<MessageBrokerConfigurer<JmsListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @SuppressFBWarnings("EI")
    public JmsMessageBrokerListenerRegistrarConfiguration(
        ConnectionFactory connectionFactory,
        @Qualifier("jacksonJmsMessageConverter") MessageConverter jacksonJmsMessageConverter,
        @Autowired(
            required = false) List<MessageBrokerConfigurer<JmsListenerEndpointRegistrar>> messageBrokerConfigurers,
        @Autowired(required = false) JmsListenerEndpointRegistry jmsListenerEndpointRegistry) {

        this.connectionFactory = connectionFactory;
        this.jacksonJmsMessageConverter = jacksonJmsMessageConverter;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
    }

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<JmsListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        JmsListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        if (logger.isTraceEnabled()) {
            logger.trace("Registering JMS Listener: {} -> {}:{}", messageRoute, delegateClass, methodName);
        }

        MessageListenerAdapter messageListenerAdapter = new NoReplyMessageListenerAdapter(delegate);

        messageListenerAdapter.setMessageConverter(jacksonJmsMessageConverter);
        messageListenerAdapter.setDefaultListenerMethod(methodName);

        SimpleJmsListenerEndpoint simpleJmsListenerEndpoint = new SimpleJmsListenerEndpoint();

        simpleJmsListenerEndpoint.setId(messageRoute + delegateClass.getSimpleName() + "Endpoint");
        simpleJmsListenerEndpoint.setDestination(messageRoute.getName());
        simpleJmsListenerEndpoint.setMessageListener(messageListenerAdapter);

        listenerEndpointRegistrar.registerEndpoint(simpleJmsListenerEndpoint, createContainerFactory(concurrency));
    }

    @Override
    public void stopListenerEndpoints() {
        try {
            if (jmsListenerEndpointRegistry != null) {
                jmsListenerEndpointRegistry.stop();
            }
        } catch (Exception e) {
            logger.warn("Failed to stop JMS listener containers: {}", e.getMessage());
        }
    }

    @Override
    public void startListenerEndpoints() {
        try {
            if (jmsListenerEndpointRegistry != null) {
                jmsListenerEndpointRegistry.start();
            }
        } catch (Exception e) {
            logger.warn("Failed to start JMS listener containers: {}", e.getMessage());
        }
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
