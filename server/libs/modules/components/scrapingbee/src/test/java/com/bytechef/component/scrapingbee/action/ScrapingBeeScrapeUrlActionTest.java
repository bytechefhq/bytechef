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

import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.OUTPUT_FORMAT;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.RETURN_PAGE_MARKDOWN;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.RETURN_PAGE_TEXT;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.URL;
import static com.bytechef.component.scrapingbee.util.ScrapingBeeTestUtils.toQueryParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
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
class ScrapingBeeScrapeUrlActionTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformHtml(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters parameters = MockParametersFactory.create(Map.of(URL, "https://example.com"));

        stubHttp(mockedResponse, mockedExecutor, mockedHttp, "<html></html>");

        String result = ScrapingBeeScrapeUrlAction.perform(parameters, parameters, mockedContext);

        assertEquals("<html></html>", result);
        assertEquals("/", stringArgumentCaptor.getValue());
        assertEquals(Map.of(URL, "https://example.com"), toQueryParameterMap(objectsArgumentCaptor.getValue()));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(ResponseType.TEXT, configurationBuilder.build()
            .getResponseType());
    }

    @Test
    void testPerformMarkdown(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters =
            MockParametersFactory.create(Map.of(URL, "https://example.com", OUTPUT_FORMAT, "MARKDOWN"));

        stubHttp(mockedResponse, mockedExecutor, mockedHttp, "# Example");

        String result = ScrapingBeeScrapeUrlAction.perform(parameters, parameters, mockedContext);

        assertEquals("# Example", result);
        assertEquals(
            Map.of(URL, "https://example.com", RETURN_PAGE_MARKDOWN, true),
            toQueryParameterMap(objectsArgumentCaptor.getValue()));
    }

    @Test
    void testPerformText(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters = MockParametersFactory.create(Map.of(URL, "https://example.com", OUTPUT_FORMAT, "TEXT"));

        stubHttp(mockedResponse, mockedExecutor, mockedHttp, "Example");

        ScrapingBeeScrapeUrlAction.perform(parameters, parameters, mockedContext);

        assertEquals(
            Map.of(URL, "https://example.com", RETURN_PAGE_TEXT, true),
            toQueryParameterMap(objectsArgumentCaptor.getValue()));
    }

    private void stubHttp(Response mockedResponse, Executor mockedExecutor, Http mockedHttp, String body) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);
    }
}
