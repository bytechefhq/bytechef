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
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LANGUAGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SESSION_ID;
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
class FirecrawlExecuteBrowserCodeActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(SESSION_ID, "session-123", CODE, "print(1)", LANGUAGE, "python", TIMEOUT, 120));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("success", true, "stdout", "1"));

        Object result = FirecrawlExecuteBrowserCodeAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("success", true, "stdout", "1"), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/interact/session-123/execute", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(CODE, "print(1)", LANGUAGE, "python", TIMEOUT, 120)), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(Duration.ofSeconds(180), configuration.getRequestTimeout());
    }

    @Test
    void testPerformWithDefaultTimeout(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(SESSION_ID, "session-123", CODE, "echo hi"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        FirecrawlExecuteBrowserCodeAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Body.of(Map.of(CODE, "echo hi")), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Duration.ofSeconds(90), configuration.getRequestTimeout());
    }
}
