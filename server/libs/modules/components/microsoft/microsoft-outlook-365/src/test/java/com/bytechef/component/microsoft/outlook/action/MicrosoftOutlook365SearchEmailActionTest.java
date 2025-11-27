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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CATEGORY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365SearchEmailActionTest {

    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FROM, "from", TO, "to", SUBJECT, "subject", CATEGORY, "cat", FORMAT, Format.FULL));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Object[]> objectArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        List<Map<String, String>> mails = List.of(Map.of(ID, "abc", SUBJECT, "subject"));

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, mails, ODATA_NEXT_LINK, "link"));

        try (MockedStatic<MicrosoftUtils> microsoftUtilsMockedStatic = mockStatic(MicrosoftUtils.class)) {
            microsoftUtilsMockedStatic
                .when(() -> MicrosoftUtils.getItemsFromNextPage(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of());

            Object result = MicrosoftOutlook365SearchEmailAction.perform(mockedParameters, null, mockedContext);

            assertEquals(mails, result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());

            assertEquals(List.of("/me/messages", "link"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());

            Object[] objects = {
                "$search", "\"from:from\" AND \"to:to\" AND \"subject:subject\" AND \"category:cat\"", "$top", 100
            };

            assertArrayEquals(objects, objectArgumentCaptor.getValue());
        }
    }
}
