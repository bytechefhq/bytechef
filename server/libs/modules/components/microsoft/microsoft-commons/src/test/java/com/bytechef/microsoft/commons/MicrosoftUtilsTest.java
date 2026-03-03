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

package com.bytechef.microsoft.commons;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.microsoft.commons.MicrosoftUtils.ID;
import static com.bytechef.microsoft.commons.MicrosoftUtils.NAME;
import static com.bytechef.microsoft.commons.MicrosoftUtils.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("some name", "abc"));
    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetFileIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(Map.of(NAME, "some name", ID, "abc", "file", "file"))));

        assertEquals(
            expectedOptions,
            MicrosoftUtils.getFileIdOptions(
                mockedParameters, null, Map.of(), "", mockedActionContext));

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.JSON, responseType);
        assertEquals("/me/drive/root/search", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetOptionsWithoutNextLink() {
        Map<String, Object> body = Map.of(
            VALUE,
            List.of(Map.of("displayName", "Team A", "id", "1")));

        List<Option<String>> result = MicrosoftUtils.getOptions(mockedContext, body, "displayName", "id");

        assertEquals(List.of(option("Team A", "1")), result);
    }

    @Test
    void testGetOptionsWithNextLinkMultiplePages(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> initialBody = Map.of(
            VALUE,
            List.of(Map.of("displayName", "Team A", "id", "1")),
            MicrosoftUtils.ODATA_NEXT_LINK, "https://graph.microsoft.com/v1.0/teams?$skiptoken=abc");

        Map<String, Object> page1 = Map.of(
            VALUE,
            List.of(Map.of("displayName", "Team B", "id", "2")),
            MicrosoftUtils.ODATA_NEXT_LINK, "https://graph.microsoft.com/v1.0/teams?$skiptoken=def");

        Map<String, Object> page2 = Map.of(
            VALUE,
            List.of(Map.of("displayName", "Team C", "id", "3")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(page1)
            .thenReturn(page2);

        List<Option<String>> result = MicrosoftUtils.getOptions(
            mockedContext, initialBody, "displayName", "id");

        List<Option<String>> expected = new ArrayList<>();

        expected.add(option("Team A", "1"));
        expected.add(option("Team B", "2"));
        expected.add(option("Team C", "3"));

        assertEquals(expected, result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        List<String> expectedUrls = List.of(
            "https://graph.microsoft.com/v1.0/teams?$skiptoken=abc",
            "https://graph.microsoft.com/v1.0/teams?$skiptoken=def");

        assertEquals(ResponseType.JSON, responseType);
        assertEquals(expectedUrls, stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetItemsFromNextPageNullLink() {
        List<Map<?, ?>> items = MicrosoftUtils.getItemsFromNextPage(null, mockedContext);

        assertTrue(items.isEmpty());
    }

    @Test
    void testProcessErrorResponseExtractsMessageFromJson() {

        when(mockedContext.json(any()))
            .thenReturn(Map.of("error", Map.of("message", "Something went wrong")));

        ProviderException providerException = MicrosoftUtils.processErrorResponse(
            400, "{\"error\":{\"message\":\"Something went wrong\"}}", Map.of(), mockedContext);

        assertEquals(400, providerException.getStatusCode());
        assertEquals("Something went wrong", providerException.getMessage());
    }

    @Test
    void testProcessErrorResponseFallbackToBodyToString() {

        when(mockedContext.json(any()))
            .thenReturn("not a map");

        ProviderException providerException = MicrosoftUtils.processErrorResponse(
            500, "raw error", Map.of(), mockedContext);

        assertEquals(500, providerException.getStatusCode());
        assertEquals("raw error", providerException.getMessage());
    }
}
