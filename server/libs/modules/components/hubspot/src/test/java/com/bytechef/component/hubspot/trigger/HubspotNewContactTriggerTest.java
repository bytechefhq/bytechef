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

package com.bytechef.component.hubspot.trigger;

import static com.bytechef.component.hubspot.constant.HubspotConstants.APP_ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.hubspot.util.HubspotUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class HubspotNewContactTriggerTest extends AbstractHubspotTriggerTest {

    @Test
    void testWebhookEnable() {
        mockedParameters = MockParametersFactory.create(Map.of(APP_ID, "appID", HAPIKEY, "hubspot api key"));

        hubspotUtilsMockedStatic.when(
            () -> HubspotUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput result = HubspotNewContactTrigger.webhookEnable(
            mockedParameters, mockedParameters, "testWebhookUrl", workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, result);
        assertEquals(
            List.of("contact.creation", "appID", "hubspot api key", "testWebhookUrl"),
            stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        mockedParameters =
            MockParametersFactory.create(Map.of(APP_ID, "appID", HAPIKEY, "hubspot api key", ID, "subscriptionId"));

        HubspotNewContactTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        hubspotUtilsMockedStatic
            .verify(() -> HubspotUtils.unsubscribeWebhook(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()));

        assertEquals(List.of("appID", "subscriptionId", "hubspot api key"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        hubspotUtilsMockedStatic.when(
            () -> HubspotUtils.extractFirstContentMap(webhookBodyArgumentCaptor.capture()))
            .thenReturn(map);

        Object result = HubspotNewContactTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(map, result);
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
    }
}
