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

package com.bytechef.component.facebook.pages.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class FacebookPagesUtilsTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetPageAccessToken(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("access_token", "pageToken", "id", "123"));

        String pageAccessToken = FacebookPagesUtils.getPageAccessToken(mockedContext, "123");

        assertEquals("pageToken", pageAccessToken);
        assertEquals(List.of("/123", "fields", "access_token"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetPageAccessTokenFailsWithoutToken(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "123"));

        assertThrows(ProviderException.class, () -> FacebookPagesUtils.getPageAccessToken(mockedContext, "123"));
    }

    @Test
    void testGetPageIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "data",
                    List.of(Map.of("id", "1", "name", "First Page"), Map.of("id", "2", "name", "Second Page"))));

        List<Option<String>> options = FacebookPagesUtils.getPageIdOptions(null, null, Map.of(), null, mockedContext);

        assertEquals(List.of(option("First Page", "1"), option("Second Page", "2")), options);
        assertEquals("/me/accounts", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            "fields", "id,name", "limit", 100
        }, objectsArgumentCaptor.getValue());
    }

    @Test
    void testExecutePageRequest(
        Response mockedResponse, Executor mockedExecutor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getStatusCode())
            .thenReturn(200);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "post"));

        Map<String, Object> body = FacebookPagesUtils.executePageRequest(mockedExecutor, "pageToken");

        assertEquals(Map.of("id", "post"), body);
        assertEquals(List.of("Authorization", "Bearer pageToken"), stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        assertTrue(configuration.isDisableAuthorization());
        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testExecutePageRequestFailsOnErrorStatus(Response mockedResponse, Executor mockedExecutor) {
        when(mockedExecutor.header(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getStatusCode())
            .thenReturn(400);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("error", Map.of("message", "Invalid OAuth access token.")));

        ProviderException providerException = assertThrows(
            ProviderException.class, () -> FacebookPagesUtils.executePageRequest(mockedExecutor, "pageToken"));

        String message = providerException.getMessage();

        assertTrue(message.contains("Invalid OAuth access token."));
    }

    @Test
    void testGetDataList() {
        List<Map<?, ?>> dataList = FacebookPagesUtils.getDataList(
            Map.of("data", List.of(Map.of("id", "1"), "ignored")));

        assertEquals(List.of(Map.of("id", "1")), dataList);

        List<Map<?, ?>> emptyDataList = FacebookPagesUtils.getDataList(null);

        assertTrue(emptyDataList.isEmpty());
    }
}
