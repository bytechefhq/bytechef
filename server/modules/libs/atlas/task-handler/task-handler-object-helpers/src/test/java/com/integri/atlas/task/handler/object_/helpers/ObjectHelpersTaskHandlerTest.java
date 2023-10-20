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

package com.integri.atlas.task.handler.object_.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ObjectHelpersTaskHandlerTest {

    private static final JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());
    private static final ObjectHelpersTaskHandler objectHelpersTaskHandler = new ObjectHelpersTaskHandler(jsonHelper);

    @Test
    public void testJSONToObject() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        String input = """
            {
                "key": 3
            }
            """;

        taskExecution.put("input", input);
        taskExecution.put("operation", "JSON_PARSE");

        assertThat(objectHelpersTaskHandler.handle(taskExecution)).isEqualTo(Map.of("key", 3));

        input =
            """
            [
                {
                    "key": 3
                }
            ]
            """;

        taskExecution.put("input", input);
        taskExecution.put("operation", "JSON_PARSE");

        assertThat(objectHelpersTaskHandler.handle(taskExecution)).isEqualTo(List.of(Map.of("key", 3)));
    }

    @Test
    public void testObjectToJSON() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        Map<String, Integer> input = Map.of("key", 3);

        taskExecution.put("input", input);
        taskExecution.put("operation", "JSON_STRINGIFY");

        assertThat(objectHelpersTaskHandler.handle(taskExecution)).isEqualTo(jsonHelper.write(input));
    }
}
