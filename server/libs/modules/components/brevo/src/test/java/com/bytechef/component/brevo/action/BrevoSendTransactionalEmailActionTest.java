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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.NAME;
import static com.bytechef.component.brevo.constant.BrevoConstants.SUBJECT;
import static com.bytechef.component.brevo.constant.BrevoConstants.TEXT_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context;
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
            "senderEmail", "sender@test.com",
            "senderName", "sender",
            "recipientEmail", "recepient@test.com",
            "recipientName", "recipient",
            SUBJECT, "test",
            TEXT_CONTENT, "this is a test."));

    @Test
    void testPerform() {
        BrevoSendTransactionalEmailAction.perform(mockedParameters, mockedParameters, mockedContext);
        Context.Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expected = Map.of(
            "sender", Map.of(EMAIL, "sender@test.com", NAME, "sender"),
            "to", List.of(Map.of(EMAIL, "recepient@test.com", NAME, "recipient")),
            SUBJECT, "test",
            TEXT_CONTENT, "this is a test.");
        assertEquals(expected, body.getContent());
    }
}
