
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

package com.bytechef.component.jsonhelper;

import static com.bytechef.component.jsonhelper.constant.JsonHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.jsonhelper.action.JsonHelperParseAction;
import com.bytechef.component.jsonhelper.action.JsonHelperStringifyAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class JsonHelperComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/jsonhelper_v1.json", new JsonHelperComponentHandler().getDefinition());
    }

    @Test
    public void testExecuteParse() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn("""
                {
                    "key": 3
                }
                """);

        assertThat(JsonHelperParseAction.executeParse(context, inputParameters))
            .isEqualTo(Map.of("key", 3));

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(
                """
                    [
                        {
                            "key": 3
                        }
                    ]
                    """);

        assertThat(JsonHelperParseAction.executeParse(context, inputParameters))
            .isEqualTo(List.of(Map.of("key", 3)));
    }

    @Test
    public void testExecuteStringify() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Map<String, Integer> input = Map.of("key", 3);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(Map.of("key", 3));

        assertThat(JsonHelperStringifyAction.executeStringify(context, inputParameters))
            .isEqualTo(JsonUtils.write(input));
    }
}
