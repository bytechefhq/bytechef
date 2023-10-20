
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
import com.bytechef.message.broker.amqp.AmqpMessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
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
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "amqp")
public class AmqpMessageBrokerConfiguration
    implements RabbitListenerConfigurer, MessageBrokerListenerRegistrar<RabbitListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(AmqpMessageBrokerConfiguration.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired(required = false)
    private List<MessageBrokerConfigurer<RabbitListenerEndpointRegistrar>> messageBrokerConfigurers = Collections
        .emptyList();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
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
            exchange = controlExchange();
            queue = controlQueue();
        } else {
            exchange = messageExchange();

            Map<String, Object> args = new HashMap<String, Object>();

            args.put("x-dead-letter-exchange", "");
            args.put("x-dead-letter-routing-key", SystemMessageRoute.DLQ.toString());

            queue = new Queue(messageRoute.toString(), true, false, false, args);
        }

        registerListenerEndpoint(listenerEndpointRegistrar, queue, exchange, concurrency, delegate, methodName);
    }

    @Bean
    AmqpMessageBroker amqpMessageBroker(AmqpTemplate amqpTemplate) {
        AmqpMessageBroker amqpMessageBroker = new AmqpMessageBroker();

        amqpMessageBroker.setAmqpTemplate(amqpTemplate);

        return amqpMessageBroker;
    }

    @Bean
    MessageConverter jacksonAmqpMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    Queue controlQueue() {
        return new Queue(SystemMessageRoute.CONTROL.toString(), true, true, true);
    }

    @Bean
    Exchange controlExchange() {
        // TODO It should probably be topic exchange: https://www.baeldung.com/java-rabbitmq-exchanges-queues-bindings

        return ExchangeBuilder.fanoutExchange(MessageRoute.Exchange.CONTROL.toString())
            .durable(true)
            .build();
    }

    @Bean
    Queue dlqQueue() {
        return new Queue(SystemMessageRoute.DLQ.toString());
    }

    @Bean
    Exchange messageExchange() {
        return ExchangeBuilder.directExchange(MessageRoute.Exchange.MESSAGE.toString())
            .durable(true)
            .build();
    }

    private SimpleRabbitListenerContainerFactory createContainerFactory(int concurrentConsumers) {
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory =
            new SimpleRabbitListenerContainerFactory();

        simpleRabbitListenerContainerFactory.setConcurrentConsumers(concurrentConsumers);
        simpleRabbitListenerContainerFactory.setConnectionFactory(connectionFactory);
        simpleRabbitListenerContainerFactory.setDefaultRequeueRejected(false);
        simpleRabbitListenerContainerFactory.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
        simpleRabbitListenerContainerFactory.setPrefetchCount(rabbitProperties.getListener()
            .getDirect()
            .getPrefetch());

        return simpleRabbitListenerContainerFactory;
    }

    private void registerListenerEndpoint(
        RabbitListenerEndpointRegistrar listenerEndpointRegistrar, Queue queue, Exchange exchange, int concurrency,
        Object delegate, String methodName) {

        rabbitAdmin(connectionFactory).declareQueue(queue);
        rabbitAdmin(connectionFactory)
            .declareBinding(BindingBuilder.bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs());

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(delegate);

        messageListenerAdapter.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
        messageListenerAdapter.setDefaultListenerMethod(methodName);

        SimpleRabbitListenerEndpoint simpleRabbitListenerEndpoint = new SimpleRabbitListenerEndpoint();

        simpleRabbitListenerEndpoint.setId(queue.getName() + "Endpoint");
        simpleRabbitListenerEndpoint.setQueueNames(queue.getName());
        simpleRabbitListenerEndpoint.setMessageListener(messageListenerAdapter);

        listenerEndpointRegistrar.registerEndpoint(simpleRabbitListenerEndpoint, createContainerFactory(concurrency));
    }
}
