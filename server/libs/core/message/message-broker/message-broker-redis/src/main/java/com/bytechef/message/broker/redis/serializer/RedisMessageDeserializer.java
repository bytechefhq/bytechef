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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public class RedisMessageDeserializer {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public RedisMessageDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object deserialize(String string) throws SerializationException {
        Assert.notNull(string, "'string' must not be null");

        try {
            RedisMessage redisMessage = objectMapper.readValue(string, RedisMessage.class);

            return objectMapper.readValue(redisMessage.payload(), Class.forName(redisMessage.type()));
        } catch (Exception e) {
            throw new SerializationException(e.getMessage());
        }
    }
}
