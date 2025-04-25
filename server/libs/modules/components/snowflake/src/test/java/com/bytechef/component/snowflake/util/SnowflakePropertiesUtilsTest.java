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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATATYPE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class SnowflakePropertiesUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());

    @Test
    void createPropertiesForColumn() {
        try (MockedStatic<SnowflakeUtils> snowflakeUtilsMockedStatic = mockStatic(SnowflakeUtils.class)) {
            snowflakeUtilsMockedStatic.when(() -> SnowflakeUtils.getTableColumns(mockedParameters, mockedActionContext))
                .thenReturn(List.of(
                    Map.of(NAME, NAME, DATATYPE, "NUMBER"),
                    Map.of(NAME, NAME, DATATYPE, "STRING"),
                    Map.of(NAME, NAME, DATATYPE, "BOOLEAN")));

            List<ValueProperty<?>> propertiesFromColumn = SnowflakePropertiesUtils.createPropertiesForColumn(
                mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ValueProperty<?>> expectedProperties = List.of(
                number(NAME)
                    .label(NAME)
                    .required(false),
                string(NAME)
                    .label(NAME)
                    .required(false),
                bool(NAME)
                    .label(NAME)
                    .required(false));

            assertEquals(expectedProperties, propertiesFromColumn);
        }
    }
}
