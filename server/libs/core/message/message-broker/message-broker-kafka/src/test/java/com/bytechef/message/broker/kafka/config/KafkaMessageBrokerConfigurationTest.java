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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class KafkaMessageBrokerConfigurationTest {

    private final MessageConverter messageConverter = new KafkaMessageBrokerConfiguration()
        .jacksonMessageConverter(JsonMapper.builder()
            .build());

    @Test
    void testResolvesAllowlistedMessageType() {
        Message<byte[]> message = createMessage("{\"name\":\"test\"}", TestEvent.class.getName());

        Object payload = messageConverter.fromMessage(message, Object.class);

        assertThat(payload).isEqualTo(new TestEvent("test"));
    }

    @Test
    void testIgnoresMessageTypeOutsideTheAllowlist() {
        Message<byte[]> message = createMessage("{\"name\":\"test\"}", TreeMap.class.getName());

        Object payload = messageConverter.fromMessage(message, Map.class);

        assertThat(payload)
            .isInstanceOf(LinkedHashMap.class)
            .isNotInstanceOf(TreeMap.class);
    }

    private static Message<byte[]> createMessage(String payload, String type) {
        return MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8))
            .setHeader("_type", type)
            .build();
    }

    record TestEvent(String name) {
    }
}
