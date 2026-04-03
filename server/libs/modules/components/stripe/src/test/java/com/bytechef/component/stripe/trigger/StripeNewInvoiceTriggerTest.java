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

package com.bytechef.component.stripe.trigger;

import static com.bytechef.component.stripe.constant.StripeConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.stripe.util.StripeUtils;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class StripeNewInvoiceTriggerTest extends AbstractStripeTriggerTest {

    @Test
    void testWebhookEnable(TriggerContext mockedContext) {
        String webhookUrl = "testWebhookUrl";

        stripeUtilsMockedStatic.when(
            () -> StripeUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput webhookEnableOutput = StripeNewInvoiceTrigger.webhookEnable(
            null, null, webhookUrl, null, mockedContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);

        assertEquals(List.of(webhookUrl, "invoice.created"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable(TriggerContext mockedContext) {
        StripeNewInvoiceTrigger.webhookDisable(
            null, null, mockedParameters, null, mockedContext);

        stripeUtilsMockedStatic
            .verify(() -> StripeUtils.unsubscribeWebhook("abc", mockedContext));
    }

    @Test
    void testWebhookRequest() {
        stripeUtilsMockedStatic.when(
            () -> StripeUtils.getNewObject(webhookBodyArgumentCaptor.capture()))
            .thenReturn(mockedObject);

        Object result = StripeNewInvoiceTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(mockedObject, result);
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
    }
}
