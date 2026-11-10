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

package com.bytechef.component.sentry.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.sentry.constant.SentryConstants.BASE_URL;
import static com.bytechef.component.sentry.constant.SentryConstants.NAME;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;
import static com.bytechef.component.sentry.constant.SentryConstants.SLUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SentryUtilsTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(ORGANIZATION, "my-org"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetBaseUriUsesTheDefaultUrl() {
        assertEquals("https://sentry.io/api/0", SentryUtils.getBaseUri(MockParametersFactory.create(Map.of())));
    }

    @Test
    void testGetBaseUriStripsTrailingSlashes() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(BASE_URL, "https://sentry.example.com//"));

        assertEquals("https://sentry.example.com/api/0", SentryUtils.getBaseUri(mockedParameters));
    }

    @Test
    void testGetIssuePath() {
        assertEquals(
            "/organizations/my-org/issues/123/", SentryUtils.getIssuePath(mockedConnectionParameters, "123"));
    }

    @Test
    void testGetProjectOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        ArgumentCaptor<Object[]> queryParametersArgumentCaptor = forClass(Object[].class);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of(NAME, "Backend", SLUG, "backend"), Map.of(NAME, "Frontend", SLUG, "frontend")));

        List<Option<String>> result = SentryUtils.getProjectOptions(
            null, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("Backend", "backend"), option("Frontend", "frontend")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/organizations/my-org/projects/", stringArgumentCaptor.getValue());
        assertEquals(List.of("per_page", "100"), Arrays.asList(queryParametersArgumentCaptor.getValue()));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
