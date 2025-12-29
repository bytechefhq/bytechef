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

package com.bytechef.message.broker.amqp.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAmqp;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.message.route.SystemMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAmqp
public class AmqpMessageBrokerListenerRegistrarConfiguration
    implements RabbitListenerConfigurer, MessageBrokerListenerRegistrar<RabbitListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(AmqpMessageBrokerListenerRegistrarConfiguration.class);

    private final ConnectionFactory connectionFactory;
    private final MessageConverter jacksonAmqpMessageConverter;
    private final List<MessageBrokerConfigurer<RabbitListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final Exchange messageExchange;
    private final Exchange controlExchange;
    private final RabbitAdmin rabbitAdmin;
    private final RabbitProperties rabbitProperties;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @SuppressFBWarnings("EI")
    public AmqpMessageBrokerListenerRegistrarConfiguration(
        ConnectionFactory connectionFactory, MessageConverter jacksonAmqpMessageConverter, @Autowired(
            required = false) List<MessageBrokerConfigurer<RabbitListenerEndpointRegistrar>> messageBrokerConfigurers,
        RabbitAdmin rabbitAdmin, RabbitProperties rabbitProperties,
        @Autowired(required = false) RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry) {

        this.connectionFactory = connectionFactory;
        this.controlExchange = createControlExchange();
        this.jacksonAmqpMessageConverter = jacksonAmqpMessageConverter;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.messageExchange = createMessageExchange();
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitProperties = rabbitProperties;
        this.rabbitListenerEndpointRegistry = rabbitListenerEndpointRegistry;
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<RabbitListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        RabbitListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        if (logger.isTraceEnabled()) {
            logger.trace("Registering AMQP Listener: {} -> {}:{}", messageRoute, delegateClass.getName(), methodName);
        }

        Exchange exchange;
        Queue queue;

        if (messageRoute.isControlExchange()) {
            exchange = controlExchange;
            queue = new Queue(messageRoute.getName(), true, true, true);
        } else {
            exchange = messageExchange;

            Map<String, Object> args = new HashMap<String, Object>();

            args.put("x-dead-letter-exchange", "");
            args.put("x-dead-letter-routing-key", SystemMessageRoute.DLQ.toString());

            queue = new Queue(messageRoute.getName(), true, false, false, args);
        }

        registerListenerEndpoint(listenerEndpointRegistrar, queue, exchange, concurrency, delegate, methodName);
    }

    Exchange createControlExchange() {
        return ExchangeBuilder.topicExchange(MessageRoute.Exchange.CONTROL.toString())
            .durable(true)
            .build();
    }

    Exchange createMessageExchange() {
        return ExchangeBuilder.directExchange(MessageRoute.Exchange.MESSAGE.toString())
            .durable(true)
            .build();
    }

    private void registerListenerEndpoint(
        RabbitListenerEndpointRegistrar listenerEndpointRegistrar, Queue queue, Exchange exchange, int concurrency,
        Object delegate, String methodName) {

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin
            .declareBinding(BindingBuilder.bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs());

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(delegate);

        messageListenerAdapter.setMessageConverter(jacksonAmqpMessageConverter);
        messageListenerAdapter.setDefaultListenerMethod(methodName);

        SimpleRabbitListenerEndpoint simpleRabbitListenerEndpoint = new SimpleRabbitListenerEndpoint();

        simpleRabbitListenerEndpoint.setId(queue.getName() + "Endpoint");
        simpleRabbitListenerEndpoint.setQueueNames(queue.getName());
        simpleRabbitListenerEndpoint.setMessageListener(messageListenerAdapter);

        listenerEndpointRegistrar.registerEndpoint(simpleRabbitListenerEndpoint, createContainerFactory(concurrency));
    }

    private SimpleRabbitListenerContainerFactory createContainerFactory(int concurrentConsumers) {
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory =
            new SimpleRabbitListenerContainerFactory();

        simpleRabbitListenerContainerFactory.setConcurrentConsumers(concurrentConsumers);
        simpleRabbitListenerContainerFactory.setConnectionFactory(connectionFactory);
        simpleRabbitListenerContainerFactory.setDefaultRequeueRejected(false);
        simpleRabbitListenerContainerFactory.setMessageConverter(jacksonAmqpMessageConverter);
        simpleRabbitListenerContainerFactory.setPrefetchCount(rabbitProperties.getListener()
            .getDirect()
            .getPrefetch());

        return simpleRabbitListenerContainerFactory;
    }

    @Override
    public void stopListenerEndpoints() {
        try {
            if (rabbitListenerEndpointRegistry != null) {
                rabbitListenerEndpointRegistry.stop();
            }
        } catch (Exception e) {
            logger.warn("Failed to stop Rabbit listener containers: {}", e.getMessage());
        }
    }

    @Override
    public void startListenerEndpoints() {
        try {
            if (rabbitListenerEndpointRegistry != null) {
                rabbitListenerEndpointRegistry.start();
            }
        } catch (Exception e) {
            logger.warn("Failed to start Rabbit listener containers: {}", e.getMessage());
        }
    }
}
