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

package com.bytechef.component.liferay.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.liferay.constant.LiferayConstants.CONTEXT_NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.DISCOVER;
import static com.bytechef.component.liferay.constant.LiferayConstants.NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICE;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICES;
import static com.bytechef.component.liferay.constant.LiferayConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class LiferayUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testGetContextNameOptions() {
        List<Option<String>> result = LiferayUtils.getContextNameOptions();

        assertEquals(getExpectedContextNameOptions(), result);
    }

    private static List<Option<String>> getExpectedContextNameOptions() {
        List<Option<String>> expectedContextNameOptions = new ArrayList<>();

        expectedContextNameOptions.add(option("PORTAL", "portal"));
        expectedContextNameOptions.add(option("ACCOUNT", "account"));
        expectedContextNameOptions.add(option("ASSET", "asset"));
        expectedContextNameOptions.add(option("ASSETLIST", "assetlist"));
        expectedContextNameOptions.add(option("AUDIT", "audit"));
        expectedContextNameOptions.add(option("BACKGROUNDTASK", "backgroundtask"));
        expectedContextNameOptions.add(option("BATCHENGINE", "batchengine"));
        expectedContextNameOptions.add(option("BLOGS", "blogs"));
        expectedContextNameOptions.add(option("CALENDAR", "calendar"));
        expectedContextNameOptions.add(option("COMMENT", "comment"));
        expectedContextNameOptions.add(option("COMMERCE", "commerce"));
        expectedContextNameOptions.add(option("CONTACT", "contact"));
        expectedContextNameOptions.add(option("CT", "ct"));
        expectedContextNameOptions.add(option("DDL", "ddl"));
        expectedContextNameOptions.add(option("DDM", "ddm"));
        expectedContextNameOptions.add(option("DEPOT", "depot"));
        expectedContextNameOptions.add(option("DISPATCH", "dispatch"));
        expectedContextNameOptions.add(option("FRAGMENT", "fragment"));
        expectedContextNameOptions.add(option("JOURNAL", "journal"));
        expectedContextNameOptions.add(option("KALEO", "kaleo"));
        expectedContextNameOptions.add(option("KALEOFORMS", "kaleoforms"));
        expectedContextNameOptions.add(option("KB", "kb"));
        expectedContextNameOptions.add(option("LAYOUT", "layout"));
        expectedContextNameOptions.add(option("LAYOUTUTILITYPAGE", "layoututilitypage"));
        expectedContextNameOptions.add(option("LISTTYPE", "listtype"));
        expectedContextNameOptions.add(option("MARKETPLACE", "marketplace"));
        expectedContextNameOptions.add(option("MB", "mb"));
        expectedContextNameOptions.add(option("NOTIFICATION", "notification"));
        expectedContextNameOptions.add(option("OAUTHCLIENT", "oauthclient"));
        expectedContextNameOptions.add(option("OBJECT", "object"));
        expectedContextNameOptions.add(option("PORTALLANGUAGEOVERRRIDE", "portallanguageoverride"));
        expectedContextNameOptions.add(option("REDIRECT", "redirect"));
        expectedContextNameOptions.add(option("REMOTEAPP", "remoteapp"));
        expectedContextNameOptions.add(option("SAP", "sap"));
        expectedContextNameOptions.add(option("SAVEDCONTENTENTRY", "savedcontententry"));
        expectedContextNameOptions.add(option("SEGMENTS", "segments"));
        expectedContextNameOptions.add(option("SHARING", "sharing"));
        expectedContextNameOptions.add(option("SITENAVIGATION", "sitenavigation"));
        expectedContextNameOptions.add(option("STYLEBOOK", "stylebook"));
        expectedContextNameOptions.add(option("SXP", "sxp"));
        expectedContextNameOptions.add(option("TRANSLATION", "translation"));
        expectedContextNameOptions.add(option("TRASH", "trash"));
        expectedContextNameOptions.add(option("WIKI", "wiki"));

        return expectedContextNameOptions;
    }

    @Test
    void testCreateParameters(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(CONTEXT_NAME, "portal", SERVICE, 1L));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(SERVICES, List.of(
                    Map.of(
                        PARAMETERS, List.of(
                            Map.of(NAME, "id", TYPE, "long"),
                            Map.of(NAME, "title", TYPE, "string"))))));

        List<Property.ValueProperty<?>> result =
            LiferayUtils.createParameters(mockedParameters, null, null, mockedContext);

        assertEquals(2, result.size());

        assertEquals("id", result.get(0)
            .getName());
        assertEquals("title", result.get(1)
            .getName());
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/api/jsonws", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            CONTEXT_NAME, "", DISCOVER, ""
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetServiceHttpData(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(SERVICES, List.of(Map.of("path", "/test", "method", "GET"))));

        Map<String, String> result = LiferayUtils.getServiceHttpData(mockedContext, "portal", 1L);

        assertEquals("GET", result.get("method"));
        assertEquals("/test", result.get("endpoint"));
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/api/jsonws", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            CONTEXT_NAME, "", DISCOVER, ""
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetServiceOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(CONTEXT_NAME, "non-portal"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("services", List.of(Map.of("name", "Service A"), Map.of("name", "Service B"))));

        Object result = LiferayUtils.getServiceOptions(mockedParameters, null, null,
            null, mockedContext);

        assertEquals(List.of(option("Service A", 1L), option("Service B", 2L)), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/api/jsonws", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            CONTEXT_NAME, "non-portal", DISCOVER, ""
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }
}
