/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.pushover.action;

import static com.bytechef.component.pushover.constant.PushoverConstants.ATTACHMENT;
import static com.bytechef.component.pushover.constant.PushoverConstants.EXPIRE;
import static com.bytechef.component.pushover.constant.PushoverConstants.MESSAGE;
import static com.bytechef.component.pushover.constant.PushoverConstants.PRIORITY;
import static com.bytechef.component.pushover.constant.PushoverConstants.RETRY;
import static com.bytechef.component.pushover.constant.PushoverConstants.TITLE;
import static com.bytechef.component.pushover.constant.PushoverConstants.TOKEN;
import static com.bytechef.component.pushover.constant.PushoverConstants.URL;
import static com.bytechef.component.pushover.constant.PushoverConstants.URL_TITLE;
import static com.bytechef.component.pushover.constant.PushoverConstants.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class PushoverSendNotificationActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Http.Executor.class);
    private final byte[] mockedAttachment = new byte[] {
        1, 2, 3
    };
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TITLE, "testTitle", MESSAGE, "This is a test message", TOKEN, "testToken", USER, "testUserKey",
            PRIORITY, "2", RETRY, 30, EXPIRE, 1800, URL, "testUrl", URL_TITLE, "testUrlTitle", ATTACHMENT,
            mockedAttachment));
    private final Response mockedResponse = mock(Response.class);

    @Test
    void testPerform() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedActionContext.file(any()))
            .thenReturn(mockedParameters.get(ATTACHMENT));
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(null);

        Object result = PushoverSendNotificationAction.perform(
            mockedParameters, mockedParameters, mockedActionContext);

        assertNull(result);

        Body body = bodyArgumentCaptor.getValue();

        if (body.getContent() instanceof Map<?, ?> bodyMap) {
            assertEquals(mockedParameters.get(TOKEN), bodyMap.get(TOKEN));
            assertEquals(mockedParameters.get(USER), bodyMap.get(USER));
            assertEquals(mockedParameters.get(MESSAGE), bodyMap.get(MESSAGE));
            assertEquals(mockedParameters.get(TITLE), bodyMap.get(TITLE));
            assertEquals(mockedParameters.get(PRIORITY), bodyMap.get(PRIORITY));
            assertEquals(mockedParameters.get(RETRY), bodyMap.get(RETRY));
            assertEquals(mockedParameters.get(EXPIRE), bodyMap.get(EXPIRE));
            assertEquals(mockedParameters.get(URL), bodyMap.get(URL));
            assertEquals(mockedParameters.get(URL_TITLE), bodyMap.get(URL_TITLE));
            assertEquals(mockedParameters.get(ATTACHMENT), bodyMap.get(ATTACHMENT));
        }
    }
}
