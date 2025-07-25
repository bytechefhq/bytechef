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

package com.bytechef.component.gotify.action;

import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRAS;
import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRA_INFO_KEY;
import static com.bytechef.component.gotify.constant.GotifyConstants.EXTRA_INFO_VALUE;
import static com.bytechef.component.gotify.constant.GotifyConstants.MESSAGE;
import static com.bytechef.component.gotify.constant.GotifyConstants.PRIORITY;
import static com.bytechef.component.gotify.constant.GotifyConstants.SUB_NAMESPACE;
import static com.bytechef.component.gotify.constant.GotifyConstants.TITLE;
import static com.bytechef.component.gotify.constant.GotifyConstants.TOP_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class GotifySendMessageActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of();
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        MESSAGE, "message", PRIORITY, 2, TITLE, "title", EXTRAS,
        List.of(Map.of(TOP_NAMESPACE, "top", SUB_NAMESPACE, "sub", EXTRA_INFO_KEY, "key", EXTRA_INFO_VALUE, "value"))));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = GotifySendMessageAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            MESSAGE, "message",
            PRIORITY, 2,
            TITLE, "title",
            EXTRAS, Map.of("top", Map.of("sub", Map.of("key", "value"))));

        assertEquals(expectedBody, body.getContent());
    }
}
