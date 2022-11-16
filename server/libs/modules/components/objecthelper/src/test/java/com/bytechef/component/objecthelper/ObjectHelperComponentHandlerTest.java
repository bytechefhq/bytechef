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

package com.bytechef.component.objecthelper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.json.JsonUtils;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final ObjectHelperComponentHandler objectHelperComponentHandler = new ObjectHelperComponentHandler();

    @Test
    public void testGetComponentDefinition() throws IOException {
        AssertUtils.assertEquals("definition/objecthelper_v1.json", new ObjectHelperComponentHandler().getDefinition());
    }

    @Test
    public void testPerformParse() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        String input = """
            {
                "key": 3
            }
            """;

        parameters.set("input", input);

        assertThat(objectHelperComponentHandler.performParse(context, parameters))
                .isEqualTo(Map.of("key", 3));

        input =
                """
            [
                {
                    "key": 3
                }
            ]
            """;

        parameters.set("input", input);

        assertThat(objectHelperComponentHandler.performParse(context, parameters))
                .isEqualTo(List.of(Map.of("key", 3)));
    }

    @Test
    public void testPerformStringify() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        Map<String, Integer> input = Map.of("key", 3);

        parameters.set("input", input);

        assertThat(objectHelperComponentHandler.performStringify(context, parameters))
                .isEqualTo(JsonUtils.write(input));
    }
}
