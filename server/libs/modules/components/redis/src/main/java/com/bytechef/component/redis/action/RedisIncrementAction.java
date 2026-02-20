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

package com.bytechef.component.redis.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.redis.constant.RedisConstants.EXPIRE;
import static com.bytechef.component.redis.constant.RedisConstants.KEY;
import static com.bytechef.component.redis.constant.RedisConstants.TTL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.redis.util.RedisUtils;
import redis.clients.jedis.Jedis;

/**
 * @author Ivica Cardic
 */
public class RedisIncrementAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("increment")
        .title("Increment")
        .description("Atomically increments a key by 1. Creates the key with value 1 if it does not exist.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The key to increment.")
                .required(true),
            bool(EXPIRE)
                .label("Expire")
                .description("Whether to set an expiration time on the key.")
                .defaultValue(false)
                .required(false),
            integer(TTL)
                .label("TTL (seconds)")
                .description("Time to live in seconds. Only used when Expire is true.")
                .defaultValue(60)
                .minValue(1)
                .required(false))
        .output(
            outputSchema(
                integer()
                    .description("The new value after incrementing.")))
        .perform(RedisIncrementAction::perform);

    private RedisIncrementAction() {
    }

    public static Long perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (Jedis jedis = RedisUtils.getJedis(connectionParameters)) {
            String key = inputParameters.getRequiredString(KEY);
            Boolean expire = inputParameters.getBoolean(EXPIRE);
            Integer ttl = inputParameters.getInteger(TTL);

            long newValue = jedis.incr(key);

            if (Boolean.TRUE.equals(expire) && ttl != null) {
                jedis.expire(key, ttl);
            }

            return newValue;
        }
    }
}
