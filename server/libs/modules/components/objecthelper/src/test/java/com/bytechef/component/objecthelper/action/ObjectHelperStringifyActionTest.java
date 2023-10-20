
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperStringifyActionTest {

    @Test
    public void testPerformStringify() {
        try (MockedStatic<MapValueUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapValueUtils.class);
            MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class)) {

            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(Map.of("key", 3));
            jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.any()))
                .thenReturn("""
                    {
                        "key": 3
                    }
                    """);

            assertThat(ObjectHelperStringifyAction.perform(Map.of(), Mockito.mock(Context.class)))
                .isEqualTo("""
                    {
                        "key": 3
                    }
                    """);
        }
    }
}
