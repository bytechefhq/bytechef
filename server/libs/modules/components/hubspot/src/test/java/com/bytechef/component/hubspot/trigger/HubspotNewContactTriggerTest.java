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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.hubspot.util.HubspotUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
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
            () -> HubspotUtils.subscribeWebhook(eventTypeArgumentCaptor.capture(), appIdArgumentCaptor.capture(),
                hapikeyArgumentCaptor.capture(), webhookUrlArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn("123");
        WebhookEnableOutput webhookEnableOutput = HubspotNewContactTrigger.webhookEnable(
            mockedParameters, mockedParameters, "testWebhookUrl", workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, "123");

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);

        assertEquals("contact.creation", eventTypeArgumentCaptor.getValue());
        assertEquals("appID", appIdArgumentCaptor.getValue());
        assertEquals("hubspot api key", hapikeyArgumentCaptor.getValue());
        assertEquals("testWebhookUrl", webhookUrlArgumentCaptor.getValue());
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
                appIdArgumentCaptor.capture(), subscriptionIdArgumentCaptor.capture(),
                hapikeyArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()));

        assertEquals("appID", appIdArgumentCaptor.getValue());
        assertEquals("subscriptionId", subscriptionIdArgumentCaptor.getValue());
        assertEquals("hubspot api key", hapikeyArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        hubspotUtilsMockedStatic.when(
            () -> HubspotUtils.extractFirstContentMap(mockedWebhookBody))
            .thenReturn(map);

        Object result = HubspotNewContactTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(map, result);
    }
}
