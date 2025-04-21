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

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class BrevoSendTransactionalEmailActionTest extends AbstractBrevoActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            SENDER_EMAIL, "sender@test.com",
            TO, List.of("recepient1@test.com", "recepient2@test.com"),
            CC, List.of("recepient1@test.com", "recepient2@test.com"),
            BCC, List.of("recepient1@test.com", "recepient2@test.com"),
            SUBJECT, "test",
            CONTENT_TYPE, BrevoSendTransactionalEmailAction.ContentType.TEXT.name(),
            CONTENT, "this is a test."));

    @Test
    void testPerform() {
        Object result = BrevoSendTransactionalEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "sender", Map.of(EMAIL, "sender@test.com"),
            TO, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            CC, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            BCC, List.of(Map.of(EMAIL, "recepient1@test.com"), Map.of(EMAIL, "recepient2@test.com")),
            SUBJECT, "test",
            "textContent", "this is a test.");

        assertEquals(expectedBody, body.getContent());
    }
}
