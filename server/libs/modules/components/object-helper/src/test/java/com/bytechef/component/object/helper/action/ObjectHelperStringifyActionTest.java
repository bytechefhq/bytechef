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

package com.bytechef.component.object.helper.action;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.object.helper.constant.ObjectHelperConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class ObjectHelperStringifyActionTest {

    @Test
    public void testPerformStringify() {
        Context context = Mockito.mock(Context.class);
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequired(Mockito.eq(ObjectHelperConstants.SOURCE)))
            .thenReturn(Map.of("key", 3));
        Mockito.when(context.json(Mockito.any()))
            .thenReturn("""
                {
                    "key": 3
                }
                """);

        assertThat(ObjectHelperStringifyAction.perform(parameterMap, parameterMap, Mockito.mock(ActionContext.class)))
            .isEqualTo("""
                {
                    "key": 3
                }
                """);
    }
}
