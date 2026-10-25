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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class AmqpMessageBrokerConfigurationTest {

    private final MessageConverter messageConverter = new AmqpMessageBrokerConfiguration()
        .jacksonAmqpMessageConverter(JsonMapper.builder()
            .build());

    @Test
    void testJacksonAmqpMessageConverterReadsMessageFromBytechefSubPackage() {
        TestEvent testEvent = new TestEvent(1L, Map.of("key", "value"));

        Message message = messageConverter.toMessage(testEvent, new MessageProperties());

        assertThat(messageConverter.fromMessage(message)).isEqualTo(testEvent);
    }

    @Test
    void testJacksonAmqpMessageConverterRejectsMessageOutsideTrustedPackages() {
        MessageProperties messageProperties = new MessageProperties();

        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader("__TypeId__", "org.example.Untrusted");

        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), messageProperties);

        assertThatThrownBy(() -> messageConverter.fromMessage(message))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("org.example.Untrusted");
    }

    record TestEvent(long id, Map<String, String> metadata) {
    }
}
