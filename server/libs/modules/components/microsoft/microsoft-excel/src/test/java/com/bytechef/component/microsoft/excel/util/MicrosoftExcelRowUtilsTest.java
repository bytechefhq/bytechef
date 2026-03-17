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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftExcelRowUtilsTest {

    private final Parameters mockedParameters =
        MockParametersFactory.create(
            Map.of(WORKBOOK_ID, 1, WORKSHEET_NAME, "test"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetRowFromWorksheet(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<String> row = List.of("value1", "value2");

        Map<String, Object> valuesMap = Map.of(VALUES, List.of(row));

        try (MockedStatic<MicrosoftExcelUtils> excelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            excelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getLastUsedColumnLabel(mockedParameters, mockedContext))
                .thenReturn("C");

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(valuesMap);

            assertEquals(row, MicrosoftExcelRowUtils.getRowFromWorksheet(mockedParameters, mockedContext, 2));

            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();

            assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
            assertEquals(
                "/me/drive/items/1/workbook/worksheets/test/range(address='A2:C2')",
                stringArgumentCaptor.getValue());
        }
    }
}
