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

import static com.bytechef.component.notion.constant.NotionConstants.DIRECTION;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.PROPERTY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.notion.util.NotionUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class NotionListDatabaseItemsActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ID, "123", PROPERTY, "checkbox", DIRECTION, "ascending"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = forClass(Boolean.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testPerform() {
        try (MockedStatic<NotionUtils> notionUtilsMockedStatic = mockStatic(NotionUtils.class)) {
            notionUtilsMockedStatic.when(() -> NotionUtils.getAllItems(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), booleanArgumentCaptor.capture(),
                objectsArgumentCaptor.capture()))
                .thenReturn(List.of(mockedObject));

            Object result = NotionListDatabaseItemsAction.perform(mockedParameters, null, mockedContext);

            assertEquals(List.of(mockedObject), result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("/data_sources/123/query", stringArgumentCaptor.getValue());
            assertFalse(booleanArgumentCaptor.getValue());

            assertArrayEquals(
                new Object[] {
                    "sorts", List.of(Map.of(PROPERTY, "checkbox", DIRECTION, "ascending"))
                },
                objectsArgumentCaptor.getValue());
        }
    }
}
