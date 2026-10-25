/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.message.route.MessageRoute;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsMessageBrokerTest {

    private static final MessageRoute CONTROL_MESSAGE_ROUTE = new MessageRoute() {

        @Override
        public Exchange getExchange() {
            return Exchange.CONTROL;
        }

        @Override
        public String getName() {
            return "task.control_events";
        }
    };

    private static final MessageRoute MESSAGE_ROUTE = new MessageRoute() {

        @Override
        public Exchange getExchange() {
            return Exchange.MESSAGE;
        }

        @Override
        public String getName() {
            return "task.events";
        }
    };

    private static final String PAYLOAD = "payload";

    private final RedisControlSender redisControlSender = mock(RedisControlSender.class);
    private final SqsTemplate sqsTemplate = mock(SqsTemplate.class);

    @Test
    @SuppressWarnings("unchecked")
    void testControlRouteGoesToRedisWhenSenderPresent() {
        AwsMessageBroker awsMessageBroker = new AwsMessageBroker(sqsTemplate, redisControlSender);

        awsMessageBroker.send(CONTROL_MESSAGE_ROUTE, PAYLOAD);

        verify(redisControlSender).send("task.control_events", PAYLOAD);
        verify(sqsTemplate, never()).sendAsync(any(Consumer.class));
        verifyNoInteractions(sqsTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMessageRouteGoesToSqsWhenSenderPresent() {
        AwsMessageBroker awsMessageBroker = new AwsMessageBroker(sqsTemplate, redisControlSender);

        awsMessageBroker.send(MESSAGE_ROUTE, PAYLOAD);

        verify(sqsTemplate).sendAsync(any(Consumer.class));
        verifyNoInteractions(redisControlSender);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testControlRouteGoesToSqsWithoutSender() {
        AwsMessageBroker awsMessageBroker = new AwsMessageBroker(sqsTemplate, null);

        awsMessageBroker.send(CONTROL_MESSAGE_ROUTE, PAYLOAD);

        verify(sqsTemplate).sendAsync(any(Consumer.class));
    }

    @Test
    void testRedisFailurePropagates() {
        RedisConnectionFailureException redisConnectionFailureException = new RedisConnectionFailureException("down");

        doThrow(redisConnectionFailureException).when(redisControlSender)
            .send("task.control_events", PAYLOAD);

        AwsMessageBroker awsMessageBroker = new AwsMessageBroker(sqsTemplate, redisControlSender);

        assertThatThrownBy(() -> awsMessageBroker.send(CONTROL_MESSAGE_ROUTE, PAYLOAD))
            .isSameAs(redisConnectionFailureException);
        verifyNoInteractions(sqsTemplate);
    }
}
