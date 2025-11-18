/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
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
        if (logger.isDebugEnabled()) {
            logger.debug("Message broker provider type enabled: aws");
        }
    }

    @Bean
    AwsMessageBroker awsMessageBroker(SqsTemplate sqsTemplate) {
        return new AwsMessageBroker(sqsTemplate);
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter() {

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

                JavaType javaType = getObjectMapper().constructType(targetClass);
                Object payload = message.getPayload();
                Class<?> view = getSerializationView(conversionHint);

                // Note: in the view case, calling withType instead of forType for
                // compatibility with Jackson <2.5
                try {
                    if (payload instanceof byte[]) {
                        if (view != null) {
                            return getObjectMapper()
                                .readerWithView(view)
                                .forType(javaType)
                                .readValue((byte[]) payload);
                        } else {
                            return getObjectMapper().readValue((byte[]) payload, javaType);
                        }
                    } else {
                        if (view != null) {
                            return getObjectMapper()
                                .readerWithView(view)
                                .forType(javaType)
                                .readValue(payload.toString());
                        } else {
                            Writer writer = new StringWriter(1024);
                            getObjectMapper().writeValue(writer, payload);
                            String payloadJSONString = writer.toString();

                            return getObjectMapper().readValue(payloadJSONString, javaType);
                        }
                    }
                } catch (IOException ex) {
                    throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
                }
            }
        };

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
