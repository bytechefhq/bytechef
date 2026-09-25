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
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ACTIVITY_TTL;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_PROFILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.NAME;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROFILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SAVE_CHANGES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TTL;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class FirecrawlCreateBrowserSessionActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Map<String, Object> profile = Map.of(NAME, "shop", SAVE_CHANGES, true);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformWithProfile(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TTL, 600, ACTIVITY_TTL, 120, INCLUDE_PROFILE, true, PROFILE, profile));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of("success", true, "id", "session-123"));

        Object result = FirecrawlCreateBrowserSessionAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("success", true, "id", "session-123"), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/interact", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(TTL, 600, ACTIVITY_TTL, 120, PROFILE, profile)), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformOmitsProfileWhenNotIncluded(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TTL, 600, INCLUDE_PROFILE, false, PROFILE, profile));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        FirecrawlCreateBrowserSessionAction.perform(mockedParameters, null, mockedContext);

        // A profile left behind the hidden Include Profile toggle must not attach the session to stored state.
        assertEquals(Body.of(Map.of(TTL, 600)), bodyArgumentCaptor.getValue());
    }
}
