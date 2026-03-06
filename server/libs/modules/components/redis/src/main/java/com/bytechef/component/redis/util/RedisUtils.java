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

package com.bytechef.component.redis.util;

import static com.bytechef.component.redis.constant.RedisConstants.DATABASE;
import static com.bytechef.component.redis.constant.RedisConstants.HOST;
import static com.bytechef.component.redis.constant.RedisConstants.PASSWORD;
import static com.bytechef.component.redis.constant.RedisConstants.PORT;

import com.bytechef.component.definition.Parameters;
import redis.clients.jedis.Jedis;

/**
 * @author Ivica Cardic
 */
public final class RedisUtils {

    private RedisUtils() {
    }

    public static Jedis getJedis(Parameters connectionParameters) {
        String host = connectionParameters.getRequiredString(HOST);
        int port = connectionParameters.getRequiredInteger(PORT);
        String password = connectionParameters.getString(PASSWORD);
        Integer database = connectionParameters.getInteger(DATABASE);

        Jedis jedis = new Jedis(host, port);

        if (password != null && !password.isEmpty()) {
            jedis.auth(password);
        }

        if (database != null) {
            jedis.select(database);
        }

        return jedis;
    }
}
