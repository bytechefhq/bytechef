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

package com.bytechef.component.mixpanel.action;

import static com.bytechef.component.mixpanel.constant.MixpanelConstants.DISTINCT_ID;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.EVENT;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.EVENTS;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.INSERT_ID;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class MixpanelTrackEventsActionTest {

    private final Map<String, Object> responseMap = Map.of("key", "value");
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(EVENTS, List.of(Map.of(EVENT, "test1", TIME, "2025-04-15T16:44:12", DISTINCT_ID, 1, INSERT_ID, 1))));

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(responseMap);

        Object result = MixpanelTrackEventsAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Body body = bodyArgumentCaptor.getValue();
        List<Map<String, Object>> expected = List.of(Map.of(
            EVENT, "test1",
            "properties", Map.of(TIME, 1744735452L, DISTINCT_ID, 1, INSERT_ID, 1)));

        assertEquals(expected, body.getContent());
        assertEquals(List.of("/import", "strict", "1"), stringArgumentCaptor.getAllValues());

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
