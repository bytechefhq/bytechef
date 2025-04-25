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

package com.bytechef.component.snowflake.action;

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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class SnowflakeInsertRowActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(DATABASE, "database", SCHEMA, "schema", TABLE, "table",
            VALUES, Map.of("col1", 5, "col2", "abc")));

    @Test
    void testPerform() {
        try (MockedStatic<SnowflakeUtils> snowflakeUtilsMockedStatic = mockStatic(SnowflakeUtils.class)) {
            snowflakeUtilsMockedStatic
                .when(() -> SnowflakeUtils.executeStatement(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = SnowflakeInsertRowAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(
                "INSERT INTO database.schema.table(col2,col1) VALUES('abc',5)",
                stringArgumentCaptor.getValue());
        }
    }
}
