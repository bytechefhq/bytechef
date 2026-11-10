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

package com.bytechef.component.facebook.pages.action;

import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.LINK;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.MESSAGE;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class FacebookPagesCreatePostActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<String> headerArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(PAGE_ID, "123", MESSAGE, "Hello", LINK, "https://bytechef.io"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(headerArgumentCaptor.capture(), headerArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("access_token", "pageToken"), Map.of("id", "123_456"));

        Map<String, Object> result = FacebookPagesCreatePostAction.perform(inputParameters, null, mockedContext);

        assertEquals(Map.of("id", "123_456"), result);
        assertEquals(List.of("/123", "/123/feed"), stringArgumentCaptor.getAllValues());
        assertEquals(List.of("Authorization", "Bearer pageToken"), headerArgumentCaptor.getAllValues());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(MESSAGE, "Hello", LINK, "https://bytechef.io"), body.getContent());
    }

    @Test
    void testPerformWithoutLink(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters inputParameters = MockParametersFactory.create(Map.of(PAGE_ID, "123", MESSAGE, "Hello"));

        when(mockedHttp.get(any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("access_token", "pageToken"), Map.of("id", "123_456"));

        FacebookPagesCreatePostAction.perform(inputParameters, null, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(MESSAGE, "Hello"), body.getContent());
    }
}
