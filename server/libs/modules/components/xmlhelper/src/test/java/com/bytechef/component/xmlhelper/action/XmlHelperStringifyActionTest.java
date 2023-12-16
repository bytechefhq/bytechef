/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.xmlhelper.action;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class XmlHelperStringifyActionTest {

    @Test
    public void testPerformStringify() {
        ActionContext context = Mockito.mock(ActionContext.class);
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(Map.of("id", 45, "name", "Poppy"));
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn("""
                {
                    "key": 3
                }
                """);

        assertThat(XmlHelperStringifyAction.perform(parameterMap, parameterMap, context))
            .isEqualTo("""
                {
                    "key": 3
                }
                """);
    }
}
