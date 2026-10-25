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

package com.bytechef.message.broker.redis.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class RedisMessageDeserializerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .build();
    private final RedisMessageDeserializer redisMessageDeserializer = new RedisMessageDeserializer(objectMapper);
    private final RedisMessageSerializer redisMessageSerializer = new RedisMessageSerializer(objectMapper);

    @Test
    void testDeserializeRejectsTypeOutsideTrustedPackages() {
        String message = objectMapper.writeValueAsString(new RedisMessage("{}", "org.example.Untrusted"));

        assertThatThrownBy(() -> redisMessageDeserializer.deserialize(message))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("org.example.Untrusted")
            .hasMessageContaining("trusted packages");
    }

    @Test
    void testDeserializeRejectsExistingTypeOutsideTrustedPackages() {
        String message = objectMapper.writeValueAsString(new RedisMessage("{}", "org.springframework.util.StopWatch"));

        assertThatThrownBy(() -> redisMessageDeserializer.deserialize(message))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("trusted packages");
    }

    @Test
    void testDeserializeReadsBytechefType() {
        TestEvent testEvent = new TestEvent("value");

        assertThat(redisMessageDeserializer.deserialize(redisMessageSerializer.serialize(testEvent)))
            .isEqualTo(testEvent);
    }

    record TestEvent(String name) {
    }
}
