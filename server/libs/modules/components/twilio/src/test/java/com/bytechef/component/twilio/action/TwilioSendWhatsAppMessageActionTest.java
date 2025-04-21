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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static com.bytechef.component.twilio.constant.TwilioConstants.USE_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class TwilioSendWhatsAppMessageActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private Parameters mockedParameters;
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Object mockedObject = mock(Object.class);

    @BeforeEach
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);
    }

    @Test
    void testPerformWithInvalidPhoneNumberFormat() {
        mockedParameters = MockParametersFactory.create(Map.of(FROM, "from", TO, "to"));

        assertThrows(IllegalArgumentException.class,
            () -> TwilioSendWhatsAppMessageAction.perform(mockedParameters, mockedParameters, mockedActionContext),
            "Invalid phone number format.");
    }

    @Test
    void testPerform() {
        mockedParameters = MockParametersFactory.create(
            Map.of(
                FROM, "whatsapp:+12345678912", TO, "whatsapp:+12345678982", USERNAME, "accountSid", USE_TEMPLATE,
                "true", CONTENT_SID, "1"));

        Object result = TwilioSendWhatsAppMessageAction.perform(
            mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            FROM, "whatsapp:+12345678912", TO, "whatsapp:+12345678982", CONTENT_SID, "1");

        assertEquals(expectedBody, body.getContent());
        assertEquals(Http.BodyContentType.FORM_URL_ENCODED, body.getContentType());
    }
}
