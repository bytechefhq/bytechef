
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

package com.bytechef.message.broker.amqp.config;

import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.MessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "amqp")
public class AmqpMessageBrokerListenerRegistrarConfiguration
    implements RabbitListenerConfigurer, MessageBrokerListenerRegistrar<RabbitListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(AmqpMessageBrokerListenerRegistrarConfiguration.class);

    private final ConnectionFactory connectionFactory;
    private final MessageConverter jacksonAmqpMessageConverter;
    private final List<MessageBrokerConfigurer<RabbitListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final Exchange messageExchange;
    private final RabbitAdmin rabbitAdmin;
    private final RabbitProperties rabbitProperties;
    private final Exchange controlExchange;
    private final Queue controlQueue;

    @SuppressFBWarnings("EI")
    public AmqpMessageBrokerListenerRegistrarConfiguration(
        ConnectionFactory connectionFactory, MessageConverter jacksonAmqpMessageConverter, @Autowired(
            required = false) List<MessageBrokerConfigurer<RabbitListenerEndpointRegistrar>> messageBrokerConfigurers,
        @Qualifier("messageExchange") Exchange messageExchange, RabbitAdmin rabbitAdmin,
        RabbitProperties rabbitProperties, @Qualifier("controlExchange") Exchange controlExchange,
        @Qualifier("controlQueue") Queue controlQueue) {

        this.connectionFactory = connectionFactory;
        this.jacksonAmqpMessageConverter = jacksonAmqpMessageConverter;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.messageExchange = messageExchange;
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitProperties = rabbitProperties;
        this.controlExchange = controlExchange;
        this.controlQueue = controlQueue;
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

        logger.info("Registering AMQP Listener: {} -> {}:{}", messageRoute, delegateClass.getName(), methodName);

        Exchange exchange;
        Queue queue;

        if (messageRoute.isControlExchange()) {
            exchange = controlExchange;
            queue = controlQueue;
        } else {
            exchange = messageExchange;

            Map<String, Object> args = new HashMap<String, Object>();

            args.put("x-dead-letter-exchange", "");
            args.put("x-dead-letter-routing-key", SystemMessageRoute.DLQ.toString());

            queue = new Queue(messageRoute.toString(), true, false, false, args);
        }

        registerListenerEndpoint(listenerEndpointRegistrar, queue, exchange, concurrency, delegate, methodName);
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
}
