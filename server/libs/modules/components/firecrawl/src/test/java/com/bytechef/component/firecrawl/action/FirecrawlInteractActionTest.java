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

package com.bytechef.component.firecrawl.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.CODE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INTERACTION_TYPE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LANGUAGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROMPT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SCRAPE_ID;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class FirecrawlInteractActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformWithPrompt(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                SCRAPE_ID, "scrape-123", INTERACTION_TYPE, PROMPT, PROMPT, "Search for iPhone 16 Pro Max",
                CODE, "stale code", LANGUAGE, "python"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("success", true, "output", "$1,199.00"));

        Object result = FirecrawlInteractAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("success", true, "output", "$1,199.00"), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/scrape/scrape-123/interact", stringArgumentCaptor.getValue());

        // A value left over from the hidden Code field must not be sent next to the prompt.
        assertEquals(Body.of(Map.of(PROMPT, "Search for iPhone 16 Pro Max")), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(Duration.ofSeconds(90), configuration.getRequestTimeout());
    }

    @Test
    void testPerformWithCode(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                SCRAPE_ID, "scrape-123", INTERACTION_TYPE, CODE, PROMPT, "stale prompt",
                CODE, "await page.click('#submit')", LANGUAGE, "node", TIMEOUT, 300));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("success", true, "exitCode", 0));

        Object result = FirecrawlInteractAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("success", true, "exitCode", 0), result);
        assertEquals("/scrape/scrape-123/interact", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(CODE, "await page.click('#submit')", LANGUAGE, "node", TIMEOUT, 300),
            bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        // The HTTP request must outlive the longest execution Firecrawl allows.
        assertEquals(Duration.ofSeconds(360), configuration.getRequestTimeout());
    }
}
