/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.ee.message.broker.aws.AwsMessageBroker;
import com.bytechef.ee.message.broker.aws.RedisControlSender;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.StringWriter;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAws
public class AwsMessageBrokerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsMessageBrokerConfiguration.class);

    public AwsMessageBrokerConfiguration(
        @Value("${bytechef.message-broker.aws.control-provider:sqs}") String controlProvider) {

        if (log.isDebugEnabled()) {
            log.debug("Message broker provider type enabled: aws");
        }

        if (!"redis".equalsIgnoreCase(controlProvider)) {
            log.warn(
                "CONTROL routes (task cancel, SSE events) use one shared SQS queue and reach a single node; set " +
                    "bytechef.message-broker.aws.control-provider=redis when running more than one node");
        }
    }

    @Bean
    AwsMessageBroker awsMessageBroker(
        SqsTemplate sqsTemplate, ObjectProvider<RedisControlSender> redisControlSenderObjectProvider) {

        return new AwsMessageBroker(sqsTemplate, redisControlSenderObjectProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.message-broker.aws", name = "control-provider", havingValue = "redis")
    RedisControlSender redisControlSender(
        RedisMessageSerializer redisMessageSerializer, StringRedisTemplate stringRedisTemplate) {

        return (channelName, message) -> stringRedisTemplate.convertAndSend(
            channelName, redisMessageSerializer.serialize(message));
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.message-broker.aws", name = "control-provider", havingValue = "redis")
    RedisMessageSerializer redisMessageSerializer(ObjectMapper objectMapper) {
        return new RedisMessageSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.message-broker.aws", name = "control-provider", havingValue = "redis")
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
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
                        log.warn("Class not found: " + type, e);
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
