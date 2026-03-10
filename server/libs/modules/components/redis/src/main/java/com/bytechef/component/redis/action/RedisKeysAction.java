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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.redis.constant.RedisConstants.KEY_PATTERN;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.redis.util.RedisUtils;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * @author Ivica Cardic
 */
public class RedisKeysAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("keys")
        .title("Keys")
        .description("Returns all keys matching a pattern.")
        .properties(
            string(KEY_PATTERN)
                .label("Key Pattern")
                .description("The pattern to match keys against (e.g., 'user:*', '*session*').")
                .defaultValue("*")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(string())
                    .description("List of keys matching the pattern.")))
        .perform(RedisKeysAction::perform);

    private RedisKeysAction() {
    }

    public static List<String> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (Jedis jedis = RedisUtils.getJedis(connectionParameters)) {
            String pattern = inputParameters.getRequiredString(KEY_PATTERN);

            ScanParams scanParams = new ScanParams().match(pattern)
                .count(100);

            List<String> keys = new ArrayList<>();
            String cursor = ScanParams.SCAN_POINTER_START;

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);

                keys.addAll(scanResult.getResult());

                cursor = scanResult.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));

            return keys;
        }
    }
}
