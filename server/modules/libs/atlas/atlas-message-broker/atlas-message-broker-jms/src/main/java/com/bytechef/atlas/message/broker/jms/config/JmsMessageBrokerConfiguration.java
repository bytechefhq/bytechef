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

import com.bytechef.atlas.config.AtlasProperties;
import com.bytechef.atlas.message.broker.Exchanges;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.atlas.message.broker.jms.JmsMessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(AtlasProperties.class)
@ConditionalOnProperty(name = "atlas.message-broker.provider", havingValue = "jms")
public class JmsMessageBrokerConfiguration
        implements JmsListenerConfigurer, MessageBrokerListenerRegistrar<JmsListenerEndpointRegistrar> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private List<MessageBrokerConfigurer> messageBrokerConfigurers;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    JmsMessageBroker jmsMessageBroker(JmsTemplate aJmsTemplate) {
        JmsMessageBroker jmsMessageBroker = new JmsMessageBroker();
        jmsMessageBroker.setJmsTemplate(aJmsTemplate);
        return jmsMessageBroker;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper aObjectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(aObjectMapper);
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
    public void configureJmsListeners(JmsListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }

        //        CoordinatorProperties coordinatorProperties = properties.getCoordinator();
        //        WorkerProperties workerProperties = properties.getWorker();

        //        if (coordinatorProperties.isEnabled()) {
        //            Coordinator coordinator = applicationContext.getBean(Coordinator.class);
        //
        //            registerListenerEndpoint(
        //                    listenerEndpointRegistrar,
        //                    Queues.COMPLETIONS,
        //                    coordinatorProperties.getSubscriptions().getCompletions(),
        //                    coordinator,
        //                    "complete");
        //            registerListenerEndpoint(
        //                    listenerEndpointRegistrar,
        //                    Queues.ERRORS,
        //                    coordinatorProperties.getSubscriptions().getErrors(),
        //                    coordinator,
        //                    "handleError");
        //            registerListenerEndpoint(
        //                    listenerEndpointRegistrar,
        //                    Queues.EVENTS,
        //                    coordinatorProperties.getSubscriptions().getEvents(),
        //                    applicationContext.getBean(EventListener.class),
        //                    "onApplicationEvent");
        //            registerListenerEndpoint(
        //                    listenerEndpointRegistrar,
        //                    Queues.JOBS,
        //                    coordinatorProperties.getSubscriptions().getJobs(),
        //                    coordinator,
        //                    "start");
        //            registerListenerEndpoint(
        //                    listenerEndpointRegistrar,
        //                    Queues.SUBFLOWS,
        //                    coordinatorProperties.getSubscriptions().getSubflows(),
        //                    coordinator,
        //                    "create");
        //        }
        //
        //        if (workerProperties.isEnabled()) {
        //            Worker worker = applicationContext.getBean(Worker.class);
        //
        //            Map<String, Object> subscriptions = workerProperties.getSubscriptions();
        //
        //            subscriptions.forEach(
        //                    (k, v) -> registerListenerEndpoint(listenerEndpointRegistrar, k, Integer.valueOf((String)
        // v), worker, "handle"));
        //
        //            registerListenerEndpoint(listenerEndpointRegistrar, Exchanges.CONTROL + "/" + Exchanges.CONTROL,
        // 1, worker, "handle");
        //        }
    }

    @Override
    public void registerListenerEndpoint(
            JmsListenerEndpointRegistrar listenerEndpointRegistrar,
            String queueName,
            int concurrency,
            Object delegate,
            String methodName) {

        if (Objects.equals(queueName, Queues.CONTROL)) {
            queueName = Exchanges.CONTROL + "/" + Exchanges.CONTROL;
        }

        logger.info(
                "Registring JMS Listener: {} -> {}:{}",
                queueName,
                delegate.getClass().getName(),
                methodName);

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
