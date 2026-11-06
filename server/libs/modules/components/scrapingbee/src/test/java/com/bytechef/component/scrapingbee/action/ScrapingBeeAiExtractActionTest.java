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

import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_EXTRACT_RULES;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_QUERY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_SELECTOR;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.EXTRACTION_TYPE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.URL;
import static com.bytechef.component.scrapingbee.util.ScrapingBeeTestUtils.toQueryParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
class ScrapingBeeAiExtractActionTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testPerformQueryReturnsPlainAnswer(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(URL, "https://example.com", EXTRACTION_TYPE, "QUERY", AI_QUERY, "price of the product",
                AI_SELECTOR, "#product"));

        stubHttp(mockedResponse, mockedExecutor, mockedHttp, "$19.99");

        Object result = ScrapingBeeAiExtractAction.perform(parameters, parameters, mockedContext);

        assertEquals("$19.99", result);
        assertEquals(
            Map.of(URL, "https://example.com", AI_QUERY, "price of the product", AI_SELECTOR, "#product"),
            toQueryParameterMap(objectsArgumentCaptor.getValue()));

        verify(mockedContext, never()).json(any());
    }

    @Test
    void testPerformRulesParsesJsonAnswer(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        String rules = "{\"price\": \"the product price\"}";

        Parameters parameters = MockParametersFactory.create(
            Map.of(URL, "https://example.com", EXTRACTION_TYPE, "RULES", AI_EXTRACT_RULES, rules,
                AI_QUERY, "ignored in rules mode"));

        Map<String, Object> parsedBody = Map.of("price", "19.99");

        stubHttp(mockedResponse, mockedExecutor, mockedHttp, " {\"price\": \"19.99\"}");

        when(mockedContext.json(any()))
            .thenReturn(parsedBody);

        Object result = ScrapingBeeAiExtractAction.perform(parameters, parameters, mockedContext);

        assertEquals(parsedBody, result);
        assertEquals(
            Map.of(URL, "https://example.com", AI_EXTRACT_RULES, rules),
            toQueryParameterMap(objectsArgumentCaptor.getValue()));
    }

    private void stubHttp(Response mockedResponse, Executor mockedExecutor, Http mockedHttp, String body) {
        when(mockedHttp.get("/"))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);
    }
}
