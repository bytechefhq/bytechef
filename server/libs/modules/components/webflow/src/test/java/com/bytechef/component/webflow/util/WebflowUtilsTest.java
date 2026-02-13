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

package com.bytechef.component.webflow.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.webflow.constant.WebflowConstants.COLLECTION_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.DISPLAY_NAME;
import static com.bytechef.component.webflow.constant.WebflowConstants.ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.ORDER_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class WebflowUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private Parameters mockedParameters;

    @Test
    void testGetItemIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<Context.ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(COLLECTION_ID, "1"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of(ID, "123", "fieldData", Map.of("name", "abc")))));

        assertEquals(
            expectedOptions,
            WebflowUtils.getItemIdOptions(
                mockedParameters, null, null, null, mockedContext));

        Context.ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();
        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/collections/1/items", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetCollectionIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<Context.ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(SITE_ID, "1"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("collections", List.of(Map.of(ID, "123", DISPLAY_NAME, "abc"))));

        assertEquals(
            expectedOptions,
            WebflowUtils.getCollectionIdOptions(
                mockedParameters, null, null, null, mockedContext));

        Context.ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();
        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/sites/1/collections", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetOrderIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<Context.ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(SITE_ID, "1"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("orders", List.of(Map.of(ORDER_ID, "123"))));

        assertEquals(
            List.of(option("123", "123")),
            WebflowUtils.getOrderIdOptions(
                mockedParameters, null, null, null, mockedContext));

        Context.ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();
        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/sites/1/orders", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetSiteIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<Context.ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = mock(Parameters.class);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("sites", List.of(Map.of(ID, "123", DISPLAY_NAME, "abc"))));

        assertEquals(
            expectedOptions,
            WebflowUtils.getSiteIdOptions(
                mockedParameters, null, null, null, mockedContext));

        Context.ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();
        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/sites", stringArgumentCaptor.getValue());
    }
}
