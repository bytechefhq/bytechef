
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.message.broker.redis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
class RedisMessageRedisSerializer implements RedisSerializer<RedisMessage> {

    private final Jackson2JsonRedisSerializer<ListItem> jackson2JsonRedisSerializer;
    private final ObjectMapper objectMapper;

    RedisMessageRedisSerializer(ObjectMapper objectMapper) {
        this.jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ListItem.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(RedisMessage redisMessage) throws SerializationException {
        Assert.notNull(redisMessage, "'object' must not be null.");

        try {
            return jackson2JsonRedisSerializer.serialize(
                new ListItem(objectMapper.writeValueAsString(redisMessage.payload()), redisMessage.payloadClassName()));
        } catch (JsonProcessingException e) {
            throw new SerializationException(e.getMessage());
        }
    }

    @Override
    @SuppressFBWarnings("NP")
    public RedisMessage deserialize(byte[] bytes) throws SerializationException {
        RedisMessage redisMessage = null;

        if (bytes != null) {
            ListItem listItem = Objects.requireNonNull(jackson2JsonRedisSerializer.deserialize(bytes));

            try {
                redisMessage = new RedisMessage(
                    objectMapper.readValue(listItem.payload(), Class.forName(listItem.type())));
            } catch (Exception e) {
                throw new SerializationException(e.getMessage());
            }
        }

        return redisMessage;
    }

    record ListItem(String payload, String type) {
    }
}
