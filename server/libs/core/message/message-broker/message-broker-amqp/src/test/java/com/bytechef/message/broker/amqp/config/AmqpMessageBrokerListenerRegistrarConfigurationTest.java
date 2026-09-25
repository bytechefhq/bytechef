/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.message.broker.amqp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.message.broker.amqp.AmqpMessageBroker;
import com.bytechef.message.route.MessageRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;

/**
 * @author Ivica Cardic
 */
class AmqpMessageBrokerListenerRegistrarConfigurationTest {

    private static final MessageRoute CONTROL_ROUTE = new TestMessageRoute(
        MessageRoute.Exchange.CONTROL, "test.control_events");

    private final RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
    private final RabbitListenerEndpointRegistrar rabbitListenerEndpointRegistrar = mock(
        RabbitListenerEndpointRegistrar.class);

    @Test
    void testTwoListenersOnAControlRouteEachGetTheirOwnQueueBoundToTheRoute() {
        AmqpMessageBrokerListenerRegistrarConfiguration amqpMessageBrokerListenerRegistrarConfiguration =
            new AmqpMessageBrokerListenerRegistrarConfiguration(
                mock(ConnectionFactory.class), mock(MessageConverter.class), List.of(), rabbitAdmin,
                new RabbitProperties(), null);

        amqpMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            rabbitListenerEndpointRegistrar, CONTROL_ROUTE, 1, new FirstDelegate(), "handle");
        amqpMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            rabbitListenerEndpointRegistrar, CONTROL_ROUTE, 1, new SecondDelegate(), "handle");

        ArgumentCaptor<SimpleRabbitListenerEndpoint> endpointArgumentCaptor = ArgumentCaptor.forClass(
            SimpleRabbitListenerEndpoint.class);

        verify(rabbitListenerEndpointRegistrar, times(2)).registerEndpoint(
            endpointArgumentCaptor.capture(), any(SimpleRabbitListenerContainerFactory.class));

        List<SimpleRabbitListenerEndpoint> endpoints = endpointArgumentCaptor.getAllValues();

        SimpleRabbitListenerEndpoint firstEndpoint = endpoints.get(0);
        SimpleRabbitListenerEndpoint secondEndpoint = endpoints.get(1);

        assertThat(firstEndpoint.getId()).isNotEqualTo(secondEndpoint.getId());
        assertThat(firstEndpoint.getQueueNames()).doesNotContainAnyElementsOf(secondEndpoint.getQueueNames());

        ArgumentCaptor<Binding> bindingArgumentCaptor = ArgumentCaptor.forClass(Binding.class);

        verify(rabbitAdmin, times(2)).declareBinding(bindingArgumentCaptor.capture());

        assertThat(bindingArgumentCaptor.getAllValues()).allSatisfy(binding -> {
            assertThat(binding.getExchange()).isEqualTo(MessageRoute.Exchange.CONTROL.toString());
            assertThat(binding.getRoutingKey()).isEqualTo(CONTROL_ROUTE.getName());
        });
    }

    @Test
    void testControlRouteMessageIsPublishedToTheControlExchange() {
        AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        AmqpMessageBroker amqpMessageBroker = new AmqpMessageBroker();

        amqpMessageBroker.setAmqpTemplate(amqpTemplate);

        amqpMessageBroker.send(CONTROL_ROUTE, "payload");

        verify(amqpTemplate).convertAndSend(
            eq(MessageRoute.Exchange.CONTROL.toString()), eq(CONTROL_ROUTE.getName()), eq("payload"),
            any(MessagePostProcessor.class));
    }

    public static class FirstDelegate {

        public void handle(String message) {
            throw new UnsupportedOperationException(message);
        }
    }

    public static class SecondDelegate {

        public void handle(String message) {
            throw new UnsupportedOperationException(message);
        }
    }

    private record TestMessageRoute(Exchange exchange, String routeName) implements MessageRoute {

        @Override
        public Exchange getExchange() {
            return exchange;
        }

        @Override
        public String getName() {
            return routeName;
        }
    }
}
