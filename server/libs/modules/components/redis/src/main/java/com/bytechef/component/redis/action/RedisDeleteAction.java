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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.redis.constant.RedisConstants.KEY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.redis.util.RedisUtils;
import redis.clients.jedis.Jedis;

/**
 * @author Ivica Cardic
 */
public class RedisDeleteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("delete")
        .title("Delete")
        .description("Deletes a key from Redis.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The key to delete.")
                .required(true))
        .output(
            outputSchema(
                bool()
                    .description("True if the key was deleted, false if it did not exist.")))
        .perform(RedisDeleteAction::perform);

    private RedisDeleteAction() {
    }

    public static Boolean perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (Jedis jedis = RedisUtils.getJedis(connectionParameters)) {
            long deleted = jedis.del(inputParameters.getRequiredString(KEY));

            return deleted > 0;
        }
    }
}
