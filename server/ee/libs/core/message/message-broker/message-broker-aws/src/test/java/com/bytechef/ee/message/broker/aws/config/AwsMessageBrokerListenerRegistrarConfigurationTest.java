/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.message.route.MessageRoute;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.support.GenericMessage;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsMessageBrokerListenerRegistrarConfigurationTest {

    private static final MessageRoute CONTROL_MESSAGE_ROUTE = new MessageRoute() {

        @Override
        public Exchange getExchange() {
            return Exchange.CONTROL;
        }

        @Override
        public String getName() {
            return "awsControlTest";
        }
    };

    private static final String PAYLOAD = "payload";

    private final EndpointRegistrar endpointRegistrar = mock(EndpointRegistrar.class);
    private final List<String> receivedPayloads = new ArrayList<>();
    private AwsMessageBrokerListenerRegistrarConfiguration awsMessageBrokerListenerRegistrarConfiguration;

    @BeforeEach
    void beforeEach() {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();

        messageHandlerMethodFactory.afterPropertiesSet();

        SqsMessageListenerContainerFactory<?> sqsMessageListenerContainerFactory =
            mock(SqsMessageListenerContainerFactory.class);

        doReturn(mock(SqsMessageListenerContainer.class)).when(sqsMessageListenerContainerFactory)
            .createContainer(any(SqsEndpoint.class));

        awsMessageBrokerListenerRegistrarConfiguration = new AwsMessageBrokerListenerRegistrarConfiguration(
            List.of(), messageHandlerMethodFactory, sqsMessageListenerContainerFactory);
    }

    @Nested
    class ControlRouteListenerReceive {

        @Test
        void testEveryHandlerRunsWhenTheFirstOneThrows() {
            registerControlListener(new FailingHandler("first handler failed"));
            registerControlListener(new RecordingHandler());

            AwsMessageBrokerListenerRegistrarConfiguration.ControlRouteListener controlRouteListener =
                getControlRouteListener();

            assertThatThrownBy(() -> controlRouteListener.receive(new GenericMessage<>(PAYLOAD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("first handler failed");

            assertThat(receivedPayloads).containsExactly(PAYLOAD);
        }

        @Test
        void testEveryFailureIsReportedWhenSeveralHandlersThrow() {
            registerControlListener(new FailingHandler("first handler failed"));
            registerControlListener(new FailingHandler("second handler failed"));
            registerControlListener(new RecordingHandler());

            AwsMessageBrokerListenerRegistrarConfiguration.ControlRouteListener controlRouteListener =
                getControlRouteListener();

            assertThatThrownBy(() -> controlRouteListener.receive(new GenericMessage<>(PAYLOAD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("first handler failed")
                .satisfies(throwable -> assertThat(throwable.getSuppressed())
                    .extracting(Throwable::getMessage)
                    .containsExactly("second handler failed"));

            assertThat(receivedPayloads).containsExactly(PAYLOAD);
        }

        @Test
        void testNoExceptionWhenEveryHandlerSucceeds() throws Exception {
            registerControlListener(new RecordingHandler());
            registerControlListener(new RecordingHandler());

            getControlRouteListener().receive(new GenericMessage<>(PAYLOAD));

            assertThat(receivedPayloads).containsExactly(PAYLOAD, PAYLOAD);
        }
    }

    private AwsMessageBrokerListenerRegistrarConfiguration.ControlRouteListener getControlRouteListener() {
        ArgumentCaptor<SqsEndpoint> sqsEndpointArgumentCaptor = ArgumentCaptor.forClass(SqsEndpoint.class);

        verify(endpointRegistrar).registerEndpoint(sqsEndpointArgumentCaptor.capture());

        SqsEndpoint sqsEndpoint = sqsEndpointArgumentCaptor.getValue();

        return (AwsMessageBrokerListenerRegistrarConfiguration.ControlRouteListener) sqsEndpoint.getBean();
    }

    private void registerControlListener(Object delegate) {
        awsMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            endpointRegistrar, CONTROL_MESSAGE_ROUTE, 1, delegate, "onEvent");
    }

    public static class FailingHandler {

        private final String errorMessage;

        FailingHandler(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public void onEvent(String payload) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public class RecordingHandler {

        public void onEvent(String payload) {
            receivedPayloads.add(payload);
        }
    }
}
