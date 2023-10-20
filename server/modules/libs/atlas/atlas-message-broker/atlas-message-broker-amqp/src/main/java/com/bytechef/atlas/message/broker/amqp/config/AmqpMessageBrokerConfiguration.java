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

package com.bytechef.atlas.message.broker.amqp.config;

import com.bytechef.atlas.config.AtlasProperties;
import com.bytechef.atlas.message.broker.Exchanges;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.amqp.AmqpMessageBroker;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 */
@Configuration
@EnableConfigurationProperties(AtlasProperties.class)
@ConditionalOnProperty(name = "atlas.message-broker.provider", havingValue = "amqp")
public class AmqpMessageBrokerConfiguration
        implements RabbitListenerConfigurer, MessageBrokerListenerRegistrar<RabbitListenerEndpointRegistrar> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private List<MessageBrokerConfigurer> messageBrokerConfigurers;

    @Autowired
    private RabbitProperties rabbitProperties;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    RabbitAdmin admin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
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
    Queue dlqQueue() {
        return new Queue(Queues.DLQ);
    }

    @Bean
    Queue controlQueue() {
        return new Queue(Queues.CONTROL, true, true, true);
    }

    @Bean
    Exchange tasksExchange() {
        return ExchangeBuilder.directExchange(Exchanges.TASKS).durable(true).build();
    }

    @Bean
    Exchange controlExchange() {
        return ExchangeBuilder.fanoutExchange(Exchanges.CONTROL).durable(true).build();
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
            RabbitListenerEndpointRegistrar listenerEndpointRegistrar,
            String queueName,
            int concurrency,
            Object delegate,
            String methodName) {
        logger.info(
                "Registring AMQP Listener: {} -> {}:{}",
                queueName,
                delegate.getClass().getName(),
                methodName);

        Exchange exchange;
        Queue queue;

        if (Objects.equals(queueName, Queues.CONTROL)) {
            exchange = controlExchange();
            queue = controlQueue();
        } else {
            exchange = tasksExchange();

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("x-dead-letter-exchange", "");
            args.put("x-dead-letter-routing-key", Queues.DLQ);

            queue = new Queue(queueName, true, false, false, args);
        }

        registerListenerEndpoint(listenerEndpointRegistrar, queue, exchange, concurrency, delegate, methodName);
    }

    private void registerListenerEndpoint(
            RabbitListenerEndpointRegistrar listenerEndpointRegistrar,
            Queue queue,
            Exchange exchange,
            int concurrency,
            Object delegate,
            String methodName) {
        admin(connectionFactory).declareQueue(queue);
        admin(connectionFactory)
                .declareBinding(BindingBuilder.bind(queue)
                        .to(exchange)
                        .with(queue.getName())
                        .noargs());

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(delegate);

        messageListenerAdapter.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
        messageListenerAdapter.setDefaultListenerMethod(methodName);

        SimpleRabbitListenerEndpoint listenerEndpoint = new SimpleRabbitListenerEndpoint();

        listenerEndpoint.setId(queue.getName() + "Endpoint");
        listenerEndpoint.setQueueNames(queue.getName());
        listenerEndpoint.setMessageListener(messageListenerAdapter);

        listenerEndpointRegistrar.registerEndpoint(listenerEndpoint, createContainerFactory(concurrency));
    }

    private SimpleRabbitListenerContainerFactory createContainerFactory(int aConcurrency) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConcurrentConsumers(aConcurrency);
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
        factory.setPrefetchCount(rabbitProperties.getListener().getDirect().getPrefetch());
        return factory;
    }
}
