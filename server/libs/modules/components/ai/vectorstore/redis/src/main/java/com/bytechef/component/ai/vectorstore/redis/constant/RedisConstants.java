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

package com.bytechef.component.ai.vectorstore.redis.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import redis.clients.jedis.JedisPooled;

/**
 * @author Monika Kušter
 */
public class RedisConstants {

    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String PUBLIC_ENDPOINT = "publicEndpoint";
    public static final String REDIS = "redis";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        String publicEndpoint = connectionParameters.getRequiredString(PUBLIC_ENDPOINT);

        int i = publicEndpoint.indexOf(":");

        JedisPooled jedisPooled = new JedisPooled(
            publicEndpoint.substring(0, i),
            Integer.parseInt(publicEndpoint.substring(i + 1)),
            connectionParameters.getRequiredString(USERNAME),
            connectionParameters.getRequiredString(PASSWORD));

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .initializeSchema(connectionParameters.getRequiredBoolean(INITIALIZE_SCHEMA))
            .build();
    };

    private RedisConstants() {
    }
}
