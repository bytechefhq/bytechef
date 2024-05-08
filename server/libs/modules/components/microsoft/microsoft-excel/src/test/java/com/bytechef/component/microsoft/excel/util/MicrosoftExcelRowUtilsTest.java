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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class MicrosoftExcelRowUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetRowFromWorksheet() {
        List<String> row = List.of("value1", "value2");

        Map<String, Object> valuesMap = Map.of(VALUES, List.of(row));

        try (MockedStatic<MicrosoftExcelUtils> excelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            excelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getLastUsedColumnLabel(mockedParameters, mockedContext))
                .thenReturn("C");

            when(mockedContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(valuesMap);

            assertEquals(row, MicrosoftExcelRowUtils.getRowFromWorksheet(mockedParameters, mockedContext, 2));
        }
    }
}
