/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAws
public class AwsMessageBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AwsMessageBrokerConfiguration.class);

    public AwsMessageBrokerConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Message broker provider type enabled: aws");
        }
    }

    @Bean
    AwsMessageBroker awsMessageBroker(SqsTemplate sqsTemplate) {
        return new AwsMessageBroker(sqsTemplate);
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();

        mappingJackson2MessageConverter.setObjectMapper(objectMapper);

        return mappingJackson2MessageConverter;
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();

        messageHandlerMethodFactory.setMessageConverter(messageConverter);

        return messageHandlerMethodFactory;
    }
}
