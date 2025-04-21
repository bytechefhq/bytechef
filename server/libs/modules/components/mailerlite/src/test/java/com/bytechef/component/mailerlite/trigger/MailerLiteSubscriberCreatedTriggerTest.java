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

package com.bytechef.component.mailerlite.trigger;

import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.mailerlite.util.MailerLiteUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class MailerLiteSubscriberCreatedTriggerTest extends AbstractsMailerLiteTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        mailerLiteUtilsMockedStatic.when(
            () -> MailerLiteUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput webhookEnableOutput = MailerLiteSubscriberCreatedTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);

        assertEquals(List.of("subscriberCreated", "subscriber.created", webhookUrl),
            stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));

        MailerLiteSubscriberCreatedTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        mailerLiteUtilsMockedStatic
            .verify(() -> MailerLiteUtils.unsubscribeWebhook("123", mockedTriggerContext));
    }

    @Test
    void testWebhookRequest() {
        Map<String, Object> content = Map.of("id", 123);

        mailerLiteUtilsMockedStatic.when(
            () -> MailerLiteUtils.getContent(webhookBodyArgumentCaptor.capture()))
            .thenReturn(content);

        Map<String, Object> result = MailerLiteSubscriberCreatedTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(content, result);
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
    }

}
