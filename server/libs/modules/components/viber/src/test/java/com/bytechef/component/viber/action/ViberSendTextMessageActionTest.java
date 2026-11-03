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

package com.bytechef.component.viber.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.viber.constant.ViberConstants.RECEIVER;
import static com.bytechef.component.viber.constant.ViberConstants.SENDER_NAME;
import static com.bytechef.component.viber.constant.ViberConstants.TEXT;
import static com.bytechef.component.viber.constant.ViberConstants.TRACKING_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockContextSetupExtension.class, ObjectMapperSetupExtension.class
})
class ViberSendTextMessageActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformSendsTextMessage(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(RECEIVER, "user-1", TEXT, "Hello", SENDER_NAME, "Support"));
        Map<String, Object> responseBody = Map.of("status", 0, "status_message", "ok", "message_token", 42);

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        Map<String, Object> result = ViberSendTextMessageAction.perform(parameters, null, mockedContext);

        assertEquals(responseBody, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/send_message", stringArgumentCaptor.getValue());
        assertEquals(
            ResponseType.JSON, configurationBuilderArgumentCaptor.getValue()
                .build()
                .getResponseType());
        assertEquals(
            Body.of(
                Map.of(
                    "receiver", "user-1", "type", "text", "sender", Map.of("name", "Support"), "text", "Hello"),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformIncludesTrackingData(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(RECEIVER, "user-1", TEXT, "Hello", SENDER_NAME, "Support", TRACKING_DATA, "order-42"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0));

        ViberSendTextMessageAction.perform(parameters, null, mockedContext);

        assertEquals(
            Body.of(
                Map.of(
                    "receiver", "user-1", "type", "text", "sender", Map.of("name", "Support"), "text", "Hello",
                    "tracking_data", "order-42"),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
