/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    MessageBroker awsMessageBroker() {
        return new AwsMessageBroker();
    }
}
