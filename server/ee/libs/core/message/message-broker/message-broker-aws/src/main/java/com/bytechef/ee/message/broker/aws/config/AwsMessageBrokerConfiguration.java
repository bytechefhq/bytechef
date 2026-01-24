/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.StringWriter;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

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
        if (logger.isDebugEnabled()) {
            logger.debug("Message broker provider type enabled: aws");
        }
    }

    @Bean
    AwsMessageBroker awsMessageBroker(SqsTemplate sqsTemplate) {
        return new AwsMessageBroker(sqsTemplate);
    }

    @Bean
    MessageConverter jacksonMessageConverter(JsonMapper objectMapper) {
        return new JacksonJsonMessageConverter(objectMapper) {

            @Override
            protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
                MessageHeaders messageHeaders = message.getHeaders();

                String type = (String) messageHeaders.get("JavaType");

                if (type != null) {
                    try {
                        targetClass = Class.forName(type);
                    } catch (ClassNotFoundException e) {
                        logger.warn("Class not found: " + type, e);
                    }
                }

                JavaType javaType = objectMapper.constructType(targetClass);
                Object payload = message.getPayload();
                Class<?> view = getSerializationView(conversionHint);

                // Note: in the view case, calling withType instead of forType for
                // compatibility with Jackson <2.5

                if (payload instanceof byte[]) {
                    if (view != null) {
                        return objectMapper.readerWithView(view)
                            .forType(javaType)
                            .readValue((byte[]) payload);
                    } else {
                        return objectMapper.readValue((byte[]) payload, javaType);
                    }
                } else if (payload instanceof String payloadString) {
                    if (view != null) {
                        return objectMapper.readerWithView(view)
                            .forType(javaType)
                            .readValue(payloadString);
                    } else {
                        return objectMapper.readValue(payloadString, javaType);
                    }
                } else {
                    Writer writer = new StringWriter(1024);

                    objectMapper.writeValue(writer, payload);

                    String payloadJSONString = writer.toString();

                    if (view != null) {
                        return objectMapper.readerWithView(view)
                            .forType(javaType)
                            .readValue(payloadJSONString);
                    } else {
                        return objectMapper.readValue(payloadJSONString, javaType);
                    }
                }
            }
        };
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();

        messageHandlerMethodFactory.setMessageConverter(messageConverter);

        return messageHandlerMethodFactory;
    }
}
