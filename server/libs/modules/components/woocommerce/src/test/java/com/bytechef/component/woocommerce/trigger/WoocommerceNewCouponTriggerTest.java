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

package com.bytechef.component.woocommerce.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.woocommerce.util.WoocommerceUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
class WoocommerceNewCouponTriggerTest extends AbstractWoocommerceTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        woocommerceUtilsMockedStatic.when(() -> WoocommerceUtils.createWebhook(
            stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedWebhookEnableOutput);

        WebhookEnableOutput webhookEnableOutput = WoocommerceNewCouponTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(mockedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(List.of(webhookUrl, "coupon.created"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        woocommerceUtilsMockedStatic.when(() -> WoocommerceUtils.deleteWebhook(
            integerArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
            .thenAnswer(Answers.RETURNS_DEFAULTS);

        WoocommerceNewCouponTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(123, integerArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        Map<String, Object> mockWebhookContent = Map.of("test", "value");

        when(mockedWebhookBody.getContent())
            .thenReturn(mockWebhookContent);

        Object result = WoocommerceNewCouponTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutputParameters, mockedTriggerContext);

        assertEquals(mockWebhookContent, result);
    }
}
