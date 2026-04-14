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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.microsoft.commons.MicrosoftConstants.ID;
import static com.bytechef.microsoft.commons.MicrosoftConstants.ODATA_NEXT_LINK;
import static com.bytechef.microsoft.commons.MicrosoftConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftExcelListWorksheetsActionTest extends AbstractMicrosoftExcelActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(WORKBOOK_ID, 1));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Map<String, ?>> worksheets = List.of(Map.of(ID, "abc"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, worksheets, ODATA_NEXT_LINK, "link"));

        try (MockedStatic<MicrosoftUtils> microsoftUtilsMockedStatic = mockStatic(MicrosoftUtils.class)) {
            microsoftUtilsMockedStatic
                .when(() -> MicrosoftUtils.getItemsFromNextPage(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of());

            List<Map<?, ?>> result = MicrosoftExcelListWorksheetsAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(worksheets, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals(
                List.of("/me/drive/items/1/workbook/worksheets", "link"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }
    }
}
