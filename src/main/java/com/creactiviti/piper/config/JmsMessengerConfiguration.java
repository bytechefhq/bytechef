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

import java.util.Map;

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

import com.creactiviti.piper.core.Coordinator;
import com.creactiviti.piper.core.Worker;
import com.creactiviti.piper.core.event.EventListener;
import com.creactiviti.piper.core.messenger.Exchanges;
import com.creactiviti.piper.core.messenger.JmsMessageBroker;
import com.creactiviti.piper.core.messenger.Queues;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(PiperProperties.class)
@ConditionalOnProperty(name="piper.messenger.provider",havingValue="jms")
public class JmsMessengerConfiguration implements JmsListenerConfigurer {

  @Autowired(required=false)
  private Worker worker;
  
  @Autowired(required=false)
  private Coordinator coordinator;

  @Autowired
  private EventListener eventListener;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private PiperProperties properties;
  
  @Autowired
  private ConnectionFactory connectionFactory;
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  @Bean
  JmsMessageBroker jmsMessenger (JmsTemplate aJmsTemplate) {
    JmsMessageBroker jmsMessenger = new JmsMessageBroker();
    jmsMessenger.setJmsTemplate(aJmsTemplate);
    return jmsMessenger;
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
  public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    return factory;
  }

  @Override
  public void configureJmsListeners (JmsListenerEndpointRegistrar aRegistrar) {
    CoordinatorProperties coordinatorProperties = properties.getCoordinator();
    WorkerProperties workerProperties = properties.getWorker();
    if(coordinatorProperties.isEnabled()) {
      registerListenerEndpoint(aRegistrar, Queues.COMPLETIONS, coordinatorProperties.getSubscriptions().getCompletions() , coordinator, "complete");
      registerListenerEndpoint(aRegistrar, Queues.ERRORS, coordinatorProperties.getSubscriptions().getErrors(), coordinator, "handleError");
      registerListenerEndpoint(aRegistrar, Queues.EVENTS, coordinatorProperties.getSubscriptions().getEvents(), eventListener, "onApplicationEvent");
      registerListenerEndpoint(aRegistrar, Queues.JOBS, coordinatorProperties.getSubscriptions().getJobs(), coordinator, "start");
      registerListenerEndpoint(aRegistrar, Queues.SUBFLOWS, coordinatorProperties.getSubscriptions().getJobs(), coordinator, "create");
    }
    if(workerProperties.isEnabled()) {
      Map<String, Object> subscriptions = workerProperties.getSubscriptions();
      subscriptions.forEach((k,v) -> registerListenerEndpoint(aRegistrar, k, Integer.valueOf((String)v), worker, "handle"));
      registerListenerEndpoint(aRegistrar, Exchanges.CONTROL+"/"+Exchanges.CONTROL, 1, worker, "handle");
    }
  }

  private void registerListenerEndpoint(JmsListenerEndpointRegistrar aRegistrar, String aQueueName, int aConcurrency, Object aDelegate, String aMethodName) {
    logger.info("Registring JMS Listener: {} -> {}:{}", aQueueName, aDelegate.getClass().getName(), aMethodName);
    
    MessageListenerAdapter messageListener = new NoReplyMessageListenerAdapter(aDelegate);
    messageListener.setMessageConverter(jacksonJmsMessageConverter(objectMapper));
    messageListener.setDefaultListenerMethod(aMethodName);

    SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
    endpoint.setId(aQueueName+"Endpoint");
    endpoint.setDestination(aQueueName);
    endpoint.setMessageListener(messageListener);

    aRegistrar.registerEndpoint(endpoint,createContainerFactory(aConcurrency));
  }

  private DefaultJmsListenerContainerFactory createContainerFactory (int aConcurrency) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConcurrency(String.valueOf(aConcurrency));
    factory.setConnectionFactory(connectionFactory);
    return factory;
  }
  
  private static class NoReplyMessageListenerAdapter extends MessageListenerAdapter {
    
    public NoReplyMessageListenerAdapter (Object aDelegate) {
      super(aDelegate);
    }
    
    @Override
    protected void handleResult(Object aResult, Message aRequest, Session aSession) {
      // ignore
    }
    
  }
}
