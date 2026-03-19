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

package com.bytechef.component.pushover.action;

import static com.bytechef.component.pushover.constant.PushoverConstants.ATTACHMENT_BASE_64;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class PushoverSendNotificationActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final byte[] fileContent = new byte[] {
        1, 2, 3
    };
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        TITLE, "testTitle", MESSAGE, "This is a test message", TOKEN, "testToken", USER, "testUserKey",
        PRIORITY, "2", RETRY, 30, EXPIRE, 1800, URL, "testUrl", URL_TITLE, "testUrlTitle",
        ATTACHMENT_BASE_64, mockedFileEntry));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String encodedToString = EncodingUtils.base64EncodeToString(fileContent);

        when(mockedContext.encoder(any()))
            .thenReturn(encodedToString);
        when(mockedContext.file(any()))
            .thenReturn(fileContent);

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        Object result = PushoverSendNotificationAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/messages.json", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            TITLE, "testTitle", MESSAGE, "This is a test message", TOKEN, "testToken", USER, "testUserKey",
            PRIORITY, "2", RETRY, 30, EXPIRE, 1800, URL, "testUrl", URL_TITLE, "testUrlTitle",
            ATTACHMENT_BASE_64, encodedToString);

        assertEquals(expectedBody, body.getContent());
    }
}
