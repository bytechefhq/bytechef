/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.config;


import java.util.HashMap;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.creactiviti.piper.core.Coordinator;
import com.creactiviti.piper.core.Worker;
import com.creactiviti.piper.core.event.EventListener;
import com.creactiviti.piper.core.messagebroker.AmqpMessageBroker;
import com.creactiviti.piper.core.messagebroker.Exchanges;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(PiperProperties.class)
@ConditionalOnProperty(name="piper.message-broker.provider",havingValue="amqp")
public class AmqpMessageBrokerConfiguration implements RabbitListenerConfigurer {
  
  @Autowired(required=false)
  private Worker worker;
  
  @Autowired(required=false)
  private Coordinator coordinator;
  
  @Autowired
  private EventListener eventListener;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private RabbitProperties rabbitProperties;
  
  @Autowired
  private PiperProperties properties;
  
  @Autowired
  private ConnectionFactory connectionFactory;
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  private static final String DEFAULT_USER = "guest";
  private static final String DEFAULT_PASS = "guest";
  private static final String DEFAULT_HOST = "localhost";
  
  @Bean
  RabbitAdmin admin (ConnectionFactory aConnectionFactory) {
    return new RabbitAdmin(aConnectionFactory);
  }
  
  @Bean
  AmqpMessageBroker amqpMessageBroker (AmqpTemplate aAmqpTemplate) {
    AmqpMessageBroker amqpMessageBroker = new AmqpMessageBroker();
    amqpMessageBroker.setAmqpTemplate(aAmqpTemplate);
    return amqpMessageBroker;
  }
  
  @Bean 
  MessageConverter jacksonAmqpMessageConverter(ObjectMapper aObjectMapper) {
    return new Jackson2JsonMessageConverter(aObjectMapper);
  }
  
  @Bean
  Queue dlqQueue () {
    return new Queue(Queues.DLQ);
  }
  
  @Bean
  Queue controlQueue () {
    return new Queue(Queues.CONTROL,true,true,true);
  }
  
  @Bean
  Exchange tasksExchange () {
    return ExchangeBuilder.directExchange(Exchanges.TASKS)
                          .delayed()
                          .durable(true)
                          .build();
  }
  
  @Bean
  Exchange controlExchange () {
    return ExchangeBuilder.fanoutExchange(Exchanges.CONTROL)
                          .durable(true)
                          .build();
  }
  
  @Override
  public void configureRabbitListeners(RabbitListenerEndpointRegistrar aRegistrar) {
    CoordinatorProperties coordinatorProperties = properties.getCoordinator();
    WorkerProperties workerProperties = properties.getWorker();
    if(coordinatorProperties.isEnabled()) {
      registerListenerEndpoint(aRegistrar, Queues.COMPLETIONS, coordinatorProperties.getSubscriptions().getCompletions() , coordinator, "complete");
      registerListenerEndpoint(aRegistrar, Queues.ERRORS, coordinatorProperties.getSubscriptions().getErrors(), coordinator, "handleError");
      registerListenerEndpoint(aRegistrar, Queues.EVENTS, coordinatorProperties.getSubscriptions().getEvents(), eventListener, "onApplicationEvent");
      registerListenerEndpoint(aRegistrar, Queues.JOBS, coordinatorProperties.getSubscriptions().getJobs(), coordinator, "start");
      registerListenerEndpoint(aRegistrar, Queues.SUBFLOWS, coordinatorProperties.getSubscriptions().getSubflows(), coordinator, "create");
    }
    if(workerProperties.isEnabled()) {
      Map<String, Object> subscriptions = workerProperties.getSubscriptions();
      subscriptions.forEach((k,v) -> registerListenerEndpoint(aRegistrar, k, Integer.valueOf((String)v), worker, "handle"));
      registerListenerEndpoint(aRegistrar, controlQueue(), controlExchange(), 1, worker, "handle");
    }
  }
  
  private void registerListenerEndpoint(RabbitListenerEndpointRegistrar aRegistrar, String aQueueName, int aConcurrency, Object aDelegate, String aMethodName) {
    logger.info("Registring AMQP Listener: {} -> {}:{}", aQueueName, aDelegate.getClass().getName(), aMethodName);

    Map<String, Object> args = new HashMap<String, Object>();
    args.put("x-dead-letter-exchange", "");
    args.put("x-dead-letter-routing-key", Queues.DLQ);
    
    Queue queue = new Queue(aQueueName, true, false, false, args);
    
    registerListenerEndpoint(aRegistrar, queue, tasksExchange(), aConcurrency, aDelegate, aMethodName);
  }
  
  private void registerListenerEndpoint(RabbitListenerEndpointRegistrar aRegistrar, Queue aQueue, Exchange aExchange, int aConcurrency, Object aDelegate, String aMethodName) {
    admin(connectionFactory).declareQueue(aQueue);
    admin(connectionFactory).declareBinding(BindingBuilder.bind(aQueue)
                                                          .to(aExchange)
                                                          .with(aQueue.getName())
                                                          .noargs());
    
    MessageListenerAdapter messageListener = new MessageListenerAdapter(aDelegate);
    messageListener.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
    messageListener.setDefaultListenerMethod(aMethodName);

    SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
    endpoint.setId(aQueue.getName()+"Endpoint");
    endpoint.setQueueNames(aQueue.getName());
    endpoint.setMessageListener(messageListener);

    aRegistrar.registerEndpoint(endpoint,createContainerFactory(aConcurrency));
  }
  
  private SimpleRabbitListenerContainerFactory createContainerFactory (int aConcurrency) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConcurrentConsumers(aConcurrency);
    factory.setConnectionFactory(connectionFactory);
    factory.setDefaultRequeueRejected(false);
    factory.setMessageConverter(jacksonAmqpMessageConverter(objectMapper));
    return factory;
  }
  
}
