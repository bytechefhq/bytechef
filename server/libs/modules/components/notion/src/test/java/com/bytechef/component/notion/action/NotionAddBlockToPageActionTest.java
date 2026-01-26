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

import static com.bytechef.component.notion.constant.NotionConstants.CAPTION;
import static com.bytechef.component.notion.constant.NotionConstants.CHECKED;
import static com.bytechef.component.notion.constant.NotionConstants.CHILDREN;
import static com.bytechef.component.notion.constant.NotionConstants.COLOR;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.EXPRESSION;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.LANGUAGE;
import static com.bytechef.component.notion.constant.NotionConstants.RICH_TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;
import static com.bytechef.component.notion.constant.NotionConstants.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class NotionAddBlockToPageActionTest {

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
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        List<Map<String, ?>> inputBlocks = List.of(
            Map.of(
                TYPE, "bookmark", URL, "https://www.bytechef.io/", CAPTION,
                List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "ByteChef")))),
            Map.of(TYPE, "breadcrumb"),
            Map.of(
                TYPE, "bulleted_list_item", COLOR, "red",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "ByteChef")))),
            Map.of(
                TYPE, "callout", COLOR, "gray",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Callout")))),
            Map.of(
                TYPE, "code", LANGUAGE, "java",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "System.out.println();")))),
            Map.of(TYPE, "embed", URL, "https://www.bytechef.io/"),
            Map.of(
                TYPE, "equation", EXPRESSION, "katex",
                CAPTION, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "equation")))),
            Map.of(TYPE, "heading_1", RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 1")))),
            Map.of(TYPE, "heading_2", RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 2")))),
            Map.of(TYPE, "heading_3", RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 3")))),
            Map.of(
                TYPE, "numbered_list_item",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Numbered List Item")))),
            Map.of(
                TYPE, "paragraph",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Paragraph")))),
            Map.of(
                TYPE, "quote",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Quote")))),
            Map.of(TYPE, "table_of_contents", COLOR, "red"),
            Map.of(
                TYPE, "to_do", CHECKED, true, COLOR, "blue",
                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of("content", "Todo 1")))),
            Map.of(TYPE, "toggle", RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Toggle")))));

        Parameters inputParameters = MockParametersFactory.create(Map.of(ID, "123", CHILDREN, inputBlocks));

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();
                return value.apply(mockedHttp);
            });
        when(mockedHttp.patch(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = NotionAddBlockToPageAction.perform(inputParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/blocks/123/children", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(
                Map.of(CHILDREN,
                    List.of(
                        Map.of(
                            "object", "block",
                            TYPE, "bookmark",
                            "bookmark", Map.of(
                                URL, "https://www.bytechef.io/",
                                CAPTION, List.of(
                                    Map.of(TYPE, "text", "text", Map.of(CONTENT, "ByteChef"))))),
                        Map.of("object", "block", TYPE, "breadcrumb", "breadcrumb", Map.of()),
                        Map.of(
                            "object", "block",
                            TYPE, "bulleted_list_item",
                            "bulleted_list_item", Map.of(
                                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "ByteChef"))),
                                COLOR, "red")),
                        Map.of(
                            "object", "block",
                            TYPE, "callout",
                            "callout", Map.of(
                                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Callout"))),
                                COLOR, "gray")),
                        Map.of(
                            "object", "block",
                            TYPE, "code",
                            "code", Map.of(
                                RICH_TEXT,
                                List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "System.out.println();"))),
                                "language", "java")),
                        Map.of("object", "block", TYPE, "embed", "embed", Map.of(URL, "https://www.bytechef.io/")),
                        Map.of("object", "block", TYPE, "equation", "equation", Map.of(EXPRESSION, "katex")),
                        Map.of("object", "block", TYPE, "heading_1", "heading_1",
                            Map.of(RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 1"))))),
                        Map.of("object", "block", TYPE, "heading_2", "heading_2",
                            Map.of(RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 2"))))),
                        Map.of("object", "block", TYPE, "heading_3", "heading_3",
                            Map.of(RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Heading 3"))))),
                        Map.of(
                            "object", "block",
                            TYPE, "numbered_list_item",
                            "numbered_list_item", Map.of(
                                RICH_TEXT,
                                List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Numbered List Item"))))),
                        Map.of(
                            "object", "block",
                            TYPE, "paragraph",
                            "paragraph", Map.of(
                                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Paragraph"))))),
                        Map.of(
                            "object", "block",
                            TYPE, "quote",
                            "quote", Map.of(
                                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Quote"))))),
                        Map.of(
                            "object", "block",
                            TYPE, "table_of_contents",
                            "table_of_contents", Map.of(COLOR, "red")),
                        Map.of(
                            "object", "block",
                            TYPE, "to_do",
                            "to_do", Map.of(
                                RICH_TEXT, List.of(Map.of(TYPE, "text", "text", Map.of(CONTENT, "Todo 1"))),
                                "checked", true,
                                COLOR, "blue")),
                        Map.of("object", "block", TYPE, "toggle", "toggle",
                            Map.of(RICH_TEXT, List.of(Map.of("text", Map.of(CONTENT, "Toggle"))))))),
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
