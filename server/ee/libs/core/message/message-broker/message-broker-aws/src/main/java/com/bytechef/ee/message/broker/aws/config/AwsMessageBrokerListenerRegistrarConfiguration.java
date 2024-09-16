/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.autoconfigure.sqs.SqsProperties;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsBeanNames;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAws
public class AwsMessageBrokerListenerRegistrarConfiguration
    implements SqsListenerConfigurer, MessageBrokerListenerRegistrar<EndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(AwsMessageBrokerListenerRegistrarConfiguration.class);

    private final BeanFactory beanFactory;
    private final List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers;
    private final MessageHandlerMethodFactory messageHandlerMethodFactory;
    private final SqsMessageListenerContainerFactory sqsMessageListenerContainerFactory;

    @SuppressFBWarnings("EI")
    public AwsMessageBrokerListenerRegistrarConfiguration(
        BeanFactory beanFactory,
        @Autowired(required = false) List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers,
        MessageHandlerMethodFactory messageHandlerMethodFactory,
        SqsMessageListenerContainerFactory sqsMessageListenerContainerFactory) {

        this.beanFactory = beanFactory;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.messageHandlerMethodFactory = messageHandlerMethodFactory;
        this.sqsMessageListenerContainerFactory = sqsMessageListenerContainerFactory;
    }

    @Override
    public void configure(EndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<EndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        EndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        logger.info("Registering AWS Listener: {} -> {}:{}", messageRoute, delegateClass.getName(), methodName);

        Method listenerMethod = Stream.of(delegateClass.getMethods())
            .filter(it -> methodName.equals(it.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No method found: " + methodName + " on " + delegate.getClass()));

        SqsEndpoint endpoint = createListenerEndpoint(messageRoute, listenerMethod);

        listenerEndpointRegistrar.registerEndpoint(endpoint);
    }

    private SqsEndpoint createListenerEndpoint(MessageRoute messageRoute, Method listenerMethod) {
        SqsEndpoint endpoint = new SqsEndpoint.SqsEndpointBuilder()
            .id(messageRoute.getName())
            .queueNames(List.of(messageRoute.getName()))
            .factoryBeanName(SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME)
            .acknowledgementMode(AcknowledgementMode.ON_SUCCESS) // change if needed
//            .maxConcurrentMessages()
//            .maxMessagesPerPoll()
//            .pollTimeoutSeconds()
//            .messageVisibility()
            .build();

        endpoint.setMethod(listenerMethod);
        endpoint.setBean(beanFactory);
        endpoint.setHandlerMethodFactory(messageHandlerMethodFactory);
        endpoint.setupContainer(sqsMessageListenerContainerFactory.createContainer(endpoint));
        return endpoint;
    }

    // the reason why the code is so messy is that it's in the testing phase

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(messageConverter);

        return messageHandlerMethodFactory;
    }

    @Bean
    protected MessageConverter messageConverter() {
        var converter = new MappingJackson2MessageConverter();
        converter.setSerializedPayloadClass(String.class);
        converter.setStrictContentTypeMatch(false);
        return converter;
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> sqsListenerContainerFactory() {
        return SqsMessageListenerContainerFactory
            .builder()
            .sqsAsyncClient(sqsAsyncClient())
            .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .build();
    }

    @Bean
    public SqsProperties.Listener listener() {
        return new SqsProperties.Listener();
    }

}
