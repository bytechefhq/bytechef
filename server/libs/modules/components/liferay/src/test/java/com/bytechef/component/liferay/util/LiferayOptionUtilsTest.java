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
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class LiferayOptionUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetApplicationsOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("/abc/openapi.{type}", Map.of());
        paths.put("/123/openapi.{type}", Map.of());

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("paths", paths));

        List<Option<String>> result = LiferayOptionUtils.getApplicationsOptions(null,
            null, null, null, mockedContext);

        assertEquals(List.of(option("abc", "abc"), option("123", "123")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/o/openapi/openapi.json", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetEndpointsOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(APPLICATION, "application"));
        Map<String, Object> methods1 = new LinkedHashMap<>();
        methods1.put("get", Map.of());
        methods1.put("post", Map.of());

        Map<String, Object> methods2 = new LinkedHashMap<>();
        methods2.put("delete", Map.of());

        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("/v1.0/sites", methods1);
        paths.put("/v1.0/accounts/{accountId}", methods2);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("info", Map.of("version", "v1.0"));
        responseBody.put("paths", paths);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        List<Option<String>> result = LiferayOptionUtils.getEndpointsOptions(mockedParameters, null,
            null, null, mockedContext);

        assertEquals(List.of(
            option("GET /v1.0/sites", "GET /sites"),
            option("POST /v1.0/sites", "POST /sites"),
            option("DELETE /v1.0/accounts/{accountId}", "DELETE /accounts/{accountId}")),
            result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/o/application/openapi.json", stringArgumentCaptor.getValue());
    }
}
