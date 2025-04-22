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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class MixpanelTrackEventsActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(EVENTS, List.of(Map.of(EVENT, "test1", TIME, "2025-04-15T16:44:12", DISTINCT_ID, 1, INSERT_ID, 1))));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(responseMap);

        Object result = MixpanelTrackEventsAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();
        List<Map<String, Object>> expected = List.of(Map.of(
            EVENT, "test1",
            "properties", Map.of(TIME, 1744735452L, DISTINCT_ID, 1, INSERT_ID, 1)));

        assertEquals(expected, body.getContent());
        assertEquals(List.of("strict", "1"), stringArgumentCaptor.getAllValues());
    }
}
