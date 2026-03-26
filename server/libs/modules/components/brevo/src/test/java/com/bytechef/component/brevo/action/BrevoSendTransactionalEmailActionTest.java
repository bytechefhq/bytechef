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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.BCC;
import static com.bytechef.component.brevo.constant.BrevoConstants.CC;
import static com.bytechef.component.brevo.constant.BrevoConstants.CONTENT;
import static com.bytechef.component.brevo.constant.BrevoConstants.CONTENT_TYPE;
import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.SENDER_EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.SUBJECT;
import static com.bytechef.component.brevo.constant.BrevoConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.brevo.action.BrevoSendTransactionalEmailAction.ContentType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class BrevoSendTransactionalEmailActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            SENDER_EMAIL, "sender@test.com",
            TO, List.of("recepient1@test.com", "recepient2@test.com"),
            CC, List.of("recepient1@test.com", "recepient2@test.com"),
            BCC, List.of("recepient1@test.com", "recepient2@test.com"),
            SUBJECT, "test",
            CONTENT_TYPE, ContentType.TEXT.name(),
            CONTENT, "this is a test."));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        Object result = BrevoSendTransactionalEmailAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/smtp/email/", stringArgumentCaptor.getValue());

        Map<String, Object> expectedBody = Map.of(
            "sender", Map.of(EMAIL, "sender@test.com"),
            TO, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            CC, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            BCC, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            SUBJECT, "test",
            "textContent", "this is a test.");

        assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}
