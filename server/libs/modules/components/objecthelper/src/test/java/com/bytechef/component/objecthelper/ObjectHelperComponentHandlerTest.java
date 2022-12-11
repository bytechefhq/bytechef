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

import static com.bytechef.component.objecthelper.constants.ObjectHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.utils.JsonUtils;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);
    private static final ObjectHelperComponentHandler objectHelperComponentHandler = new ObjectHelperComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
                "definition/objecthelper_v1.json", new ObjectHelperComponentHandler().getDefinition());
    }

    @Test
    public void testPerformParse() {
        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn("""
            {
                "key": 3
            }
            """);

        assertThat(objectHelperComponentHandler.performParse(context, executionParameters))
                .isEqualTo(Map.of("key", 3));

        executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn(
                        """
            [
                {
                    "key": 3
                }
            ]
            """);

        assertThat(objectHelperComponentHandler.performParse(context, executionParameters))
                .isEqualTo(List.of(Map.of("key", 3)));
    }

    @Test
    public void testPerformStringify() {
        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Map<String, Integer> input = Map.of("key", 3);

        Mockito.when(executionParameters.getRequired(SOURCE)).thenReturn(Map.of("key", 3));

        assertThat(objectHelperComponentHandler.performStringify(context, executionParameters))
                .isEqualTo(JsonUtils.write(input));
    }
}
