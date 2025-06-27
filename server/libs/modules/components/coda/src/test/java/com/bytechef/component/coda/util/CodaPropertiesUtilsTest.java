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

package com.bytechef.component.coda.util;

import static com.bytechef.component.coda.action.CodaInsertRowAction.DOC_ID;
import static com.bytechef.component.coda.action.CodaInsertRowAction.ROW_VALUES;
import static com.bytechef.component.coda.action.CodaInsertRowAction.TABLE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class CodaPropertiesUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);

    @Test
    void testCreatePropertiesForRowValues() {

        Parameters parameters = MockParametersFactory.create(Map.of(DOC_ID, "1", TABLE_ID, "2",
            ROW_VALUES, Map.of("3", "test")));

        Map<String, Object> column1 = Map.of(
            "name", "Text",
            "format", Map.of("type", "text"));

        Map<String, Object> column2 = Map.of(
            "name", "Date",
            "format", Map.of("type", "date"));

        Map<String, Object> column3 = Map.of(
            "name", "Person",
            "format", Map.of("type", "person"));

        List<Map<String, Object>> items = List.of(column1, column2, column3);
        Map<String, Object> mockedResponseBody = Map.of("items", items);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedResponseBody);

        List<Property.ValueProperty<?>> result = CodaPropertiesUtils.createPropertiesForRowValues(
            parameters, parameters, new HashMap<>(), mockedActionContext);

        assertEquals("Text", result.get(0)
            .getName());
        assertEquals(Property.ControlType.TEXT, result.get(0)
            .getControlType());

        assertEquals("Date", result.get(1)
            .getName());
        assertEquals(Property.Type.DATE, result.get(1)
            .getType());

        assertEquals("Person", result.get(2)
            .getName());
        assertEquals(Property.Type.STRING, result.get(2)
            .getType());
    }
}
