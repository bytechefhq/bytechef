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
import static com.bytechef.component.redis.constant.RedisConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.redis.util.RedisUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

/**
 * @author Ivica Cardic
 */
public class RedisSetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("set")
        .title("Set")
        .description("Sets the value of a key in Redis.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The key to set.")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to store.")
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
                bool()
                    .description("True if the value was set successfully.")))
        .perform(RedisSetAction::perform);

    private RedisSetAction() {
    }

    public static Boolean perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (Jedis jedis = RedisUtils.getJedis(connectionParameters)) {
            String key = inputParameters.getRequiredString(KEY);
            String value = inputParameters.getRequiredString(VALUE);
            Boolean expire = inputParameters.getBoolean(EXPIRE);
            Integer ttl = inputParameters.getInteger(TTL);

            String result;

            if (Boolean.TRUE.equals(expire) && ttl != null) {
                result = jedis.set(key, value, SetParams.setParams()
                    .ex(ttl));
            } else {
                result = jedis.set(key, value);
            }

            return "OK".equals(result);
        }
    }
}
