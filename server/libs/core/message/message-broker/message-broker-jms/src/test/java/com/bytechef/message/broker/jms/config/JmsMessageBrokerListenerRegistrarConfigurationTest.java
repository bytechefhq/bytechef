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

package com.bytechef.message.broker.jms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.message.route.MessageRoute;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * @author Ivica Cardic
 */
class JmsMessageBrokerListenerRegistrarConfigurationTest {

    private static final MessageRoute CONTROL_ROUTE = new TestMessageRoute(
        MessageRoute.Exchange.CONTROL, "test.control_events");

    @Test
    void testTwoListenersOnAControlRouteEachSubscribeToTheTopic() {
        JmsListenerEndpointRegistrar jmsListenerEndpointRegistrar = mock(JmsListenerEndpointRegistrar.class);

        JmsMessageBrokerListenerRegistrarConfiguration jmsMessageBrokerListenerRegistrarConfiguration =
            new JmsMessageBrokerListenerRegistrarConfiguration(
                mock(ConnectionFactory.class), mock(MessageConverter.class), List.of(), null);

        jmsMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            jmsListenerEndpointRegistrar, CONTROL_ROUTE, 1, new FirstDelegate(), "handle");
        jmsMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            jmsListenerEndpointRegistrar, CONTROL_ROUTE, 1, new SecondDelegate(), "handle");

        ArgumentCaptor<SimpleJmsListenerEndpoint> endpointArgumentCaptor = ArgumentCaptor.forClass(
            SimpleJmsListenerEndpoint.class);
        ArgumentCaptor<DefaultJmsListenerContainerFactory> containerFactoryArgumentCaptor = ArgumentCaptor.forClass(
            DefaultJmsListenerContainerFactory.class);

        verify(jmsListenerEndpointRegistrar, times(2)).registerEndpoint(
            endpointArgumentCaptor.capture(), containerFactoryArgumentCaptor.capture());

        List<SimpleJmsListenerEndpoint> endpoints = endpointArgumentCaptor.getAllValues();

        SimpleJmsListenerEndpoint firstEndpoint = endpoints.get(0);
        SimpleJmsListenerEndpoint secondEndpoint = endpoints.get(1);

        assertThat(firstEndpoint.getId()).isNotEqualTo(secondEndpoint.getId());

        List<DefaultJmsListenerContainerFactory> containerFactories = containerFactoryArgumentCaptor.getAllValues();

        for (int index = 0; index < containerFactories.size(); index++) {
            DefaultJmsListenerContainerFactory containerFactory = containerFactories.get(index);

            DefaultMessageListenerContainer messageListenerContainer = containerFactory.createListenerContainer(
                endpoints.get(index));

            assertThat(messageListenerContainer.isPubSubDomain()).isTrue();
        }
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
