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

package com.bytechef.message.broker.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.message.route.MessageRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @author Ivica Cardic
 */
class KafkaMessageBrokerListenerRegistrarConfigurationTest {

    private static final MessageRoute CONTROL_ROUTE = new TestMessageRoute(
        MessageRoute.Exchange.CONTROL, "test.control_events");

    @Test
    void testTwoListenersOnAControlRouteEachConsumeInTheirOwnGroup() {
        KafkaListenerEndpointRegistrar kafkaListenerEndpointRegistrar = mock(KafkaListenerEndpointRegistrar.class);

        KafkaMessageBrokerListenerRegistrarConfiguration kafkaMessageBrokerListenerRegistrarConfiguration =
            new KafkaMessageBrokerListenerRegistrarConfiguration(
                mock(BeanFactory.class), List.of(), mock(MessageHandlerMethodFactory.class), null);

        kafkaMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            kafkaListenerEndpointRegistrar, CONTROL_ROUTE, 1, new FirstDelegate(), "handle");
        kafkaMessageBrokerListenerRegistrarConfiguration.registerListenerEndpoint(
            kafkaListenerEndpointRegistrar, CONTROL_ROUTE, 1, new SecondDelegate(), "handle");

        ArgumentCaptor<KafkaListenerEndpoint> endpointArgumentCaptor = ArgumentCaptor.forClass(
            KafkaListenerEndpoint.class);

        verify(kafkaListenerEndpointRegistrar, times(2)).registerEndpoint(endpointArgumentCaptor.capture());

        List<KafkaListenerEndpoint> endpoints = endpointArgumentCaptor.getAllValues();

        KafkaListenerEndpoint firstEndpoint = endpoints.get(0);
        KafkaListenerEndpoint secondEndpoint = endpoints.get(1);

        assertThat(firstEndpoint.getId()).isNotEqualTo(secondEndpoint.getId());
        assertThat(firstEndpoint.getGroupId()).isNotNull();
        assertThat(secondEndpoint.getGroupId()).isNotNull();
        assertThat(firstEndpoint.getGroupId()).isNotEqualTo(secondEndpoint.getGroupId());
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
