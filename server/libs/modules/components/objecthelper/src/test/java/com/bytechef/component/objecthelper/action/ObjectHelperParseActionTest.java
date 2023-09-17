
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

package com.bytechef.component.objecthelper.action;

import com.bytechef.hermes.component.definition.Context;

import com.bytechef.hermes.component.definition.ParameterMap;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperParseActionTest {

    @Test
    public void testPerformParse() {
        Context context = Mockito.mock(Context.class);
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(
                """
                    {
                        "key": 3
                    }
                    """);
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(Map.of("key", 3));

        assertThat(ObjectHelperParseAction.perform(parameterMap, parameterMap, context))
            .isEqualTo(Map.of("key", 3));

        Mockito.when(parameterMap.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(
                """
                    [
                        {
                            "key": 3
                        }
                    ]
                    """);
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(List.of(Map.of("key", 3)));

        assertThat(ObjectHelperParseAction.perform(parameterMap, parameterMap, context))
            .isEqualTo(List.of(Map.of("key", 3)));
    }
}
