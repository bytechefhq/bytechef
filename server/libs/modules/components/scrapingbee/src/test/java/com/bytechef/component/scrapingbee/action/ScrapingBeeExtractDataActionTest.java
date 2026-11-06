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

package com.bytechef.component.scrapingbee.action;

import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.EXTRACT_RULES;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.URL;
import static com.bytechef.component.scrapingbee.util.ScrapingBeeTestUtils.toQueryParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class ScrapingBeeExtractDataActionTest {

    private static final String EXTRACT_RULES_JSON = "{\"title\": \"h1\"}";

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(URL, "https://example.com", EXTRACT_RULES, EXTRACT_RULES_JSON));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> responseBody = Map.of("title", "Example Domain");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(responseBody);

        Object result = ScrapingBeeExtractDataAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseBody, result);
        assertEquals("/", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(URL, "https://example.com", EXTRACT_RULES, EXTRACT_RULES_JSON),
            toQueryParameterMap(objectsArgumentCaptor.getValue()));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(ResponseType.JSON, configurationBuilder.build()
            .getResponseType());
    }
}
