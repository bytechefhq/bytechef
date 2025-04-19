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

package com.bytechef.component.snowflake.action;

import static com.bytechef.component.snowflake.constant.SnowflakeConstants.COLUMN;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.CONDITION;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.snowflake.util.SnowflakeUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class SnowflakeUpdateRowActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            DATABASE, "database", SCHEMA, "schema", TABLE, "table", COLUMN, "col1",
            CONDITION, "2", VALUES, "5,5"));

    @Test
    void perform() {
        try (MockedStatic<SnowflakeUtils> snowflakeUtilsMockedStatic = mockStatic(SnowflakeUtils.class)) {
            snowflakeUtilsMockedStatic
                .when(() -> SnowflakeUtils.getTableColumns(
                    parametersArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn("col1,col2");
            snowflakeUtilsMockedStatic
                .when(() -> SnowflakeUtils.getColumnUpdateStatement(
                    stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn("col1=5,col2=5");
            snowflakeUtilsMockedStatic
                .when(() -> SnowflakeUtils.executeStatement(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = SnowflakeUpdateRowAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(mockedContext, mockedContext), contextArgumentCaptor.getAllValues());
            assertEquals(
                List.of("col1,col2", "5,5", "UPDATE database.schema.table SET col1=5,col2=5 WHERE col1 = 2"),
                stringArgumentCaptor.getAllValues());
        }
    }
}
