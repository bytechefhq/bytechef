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

package com.bytechef.component.notion.action;

import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.notion.util.NotionUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class NotionGetDatabaseActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);

    @Test
    void testPerform() {
        try (MockedStatic<NotionUtils> notionUtilsMockedStatic = mockStatic(NotionUtils.class)) {
            notionUtilsMockedStatic
                .when(() -> NotionUtils.getDatabase(stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(Map.of());

            Object result = NotionGetDatabaseAction.perform(mockedParameters, null, mockedContext);

            assertEquals(Map.of(), result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("123", stringArgumentCaptor.getValue());
        }
    }
}
