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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.redis.constant.RedisConstants.LIST;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.redis.util.RedisUtils;
import redis.clients.jedis.Jedis;

/**
 * @author Ivica Cardic
 */
public class RedisListLengthAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listLength")
        .title("List Length")
        .description("Returns the length of a Redis list.")
        .properties(
            string(LIST)
                .label("List")
                .description("The name of the list.")
                .required(true))
        .output(
            outputSchema(
                integer()
                    .description("The length of the list, or 0 if the key does not exist.")))
        .perform(RedisListLengthAction::perform);

    private RedisListLengthAction() {
    }

    public static Long perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (Jedis jedis = RedisUtils.getJedis(connectionParameters)) {
            return jedis.llen(inputParameters.getRequiredString(LIST));
        }
    }
}
