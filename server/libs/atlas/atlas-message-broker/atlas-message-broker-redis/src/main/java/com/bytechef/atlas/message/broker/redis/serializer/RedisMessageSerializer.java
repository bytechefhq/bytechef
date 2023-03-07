
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

package com.bytechef.atlas.message.broker.redis.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class RedisMessageSerializer {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public RedisMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object object) throws SerializationException {
        Assert.notNull(object, "'object' must not be null");

        try {
            Class<?> messageClass = object.getClass();

            return objectMapper.writeValueAsString(
                new RedisMessage(objectMapper.writeValueAsString(object), messageClass.getName()));
        } catch (JsonProcessingException e) {
            throw new SerializationException(e.getMessage());
        }
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

    record RedisMessage(String payload, String type) {
    }
}
