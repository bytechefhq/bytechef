/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerJms;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerJms
public class AwsMessageBrokerListenerRegistrarConfiguration
    implements SqsListenerConfigurer, MessageBrokerListenerRegistrar<EndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(AwsMessageBrokerListenerRegistrarConfiguration.class);

    private final List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers;

    @SuppressFBWarnings("EI")
    public AwsMessageBrokerListenerRegistrarConfiguration(
        @Autowired(required = false) List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
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

        logger.info("Registering AWS Listener: {} -> {}:{}", messageRoute, delegateClass, methodName);

        // TODO
    }
}
