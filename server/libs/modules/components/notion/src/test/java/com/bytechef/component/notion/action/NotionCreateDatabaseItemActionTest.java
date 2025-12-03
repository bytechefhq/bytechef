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

import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.FIELDS;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
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
class NotionCreateDatabaseItemActionTest {

    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ID, "123", FIELDS, Map.of("checkbox", true), CONTENT, "A"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, ?>> mapArgumentCaptor = forClass(Map.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        try (MockedStatic<NotionUtils> notionUtilsMockedStatic = mockStatic(NotionUtils.class)) {
            notionUtilsMockedStatic
                .when(() -> NotionUtils.convertPropertiesToNotionValues(contextArgumentCaptor.capture(),
                    mapArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(Map.of("ABC", Map.of("checkbox", true)));

            when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                    return value.apply(mockedHttp);
                });
            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(mockedObject);

            Object result = NotionCreateDatabaseItemAction.perform(mockedParameters, null, mockedContext);

            assertEquals(mockedObject, result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Http.Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(Map.of("checkbox", true), mapArgumentCaptor.getValue());
            assertEquals(List.of("123", "/pages"), stringArgumentCaptor.getAllValues());

            Map<String, Object> expectedBody = Map.of(
                "parent", Map.of(TYPE, "database_id", "database_id", "123"),
                "properties", Map.of("ABC", Map.of("checkbox", true)),
                "children", List.of(Map.of("object", "block", TYPE, "paragraph", "paragraph",
                    Map.of("rich_text", List.of(Map.of(TYPE, TEXT, TEXT, Map.of(CONTENT, "A")))))));

            assertEquals(Http.Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
        }
    }
}
