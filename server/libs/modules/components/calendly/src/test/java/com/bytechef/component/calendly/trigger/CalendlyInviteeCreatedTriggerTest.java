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

package com.bytechef.component.calendly.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.calendly.util.CalendlyUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class CalendlyInviteeCreatedTriggerTest extends AbstractCalendlyTriggerTest {

    @Test
    void testWebhookDisable() {
        calendlyUtilsMockedStatic.when(
            () -> CalendlyUtils.unsubscribeWebhook(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenAnswer((Answer<Void>) invocation -> null);

        CalendlyInviteeCreatedTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
        assertEquals("123", stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        calendlyUtilsMockedStatic.when(
            () -> CalendlyUtils.subscribeWebhook(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(mockedWebhookEnableOutput);

        WebhookEnableOutput webhookEnableOutput = CalendlyInviteeCreatedTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        assertEquals(mockedWebhookEnableOutput, webhookEnableOutput);

        assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
        assertEquals(List.of(webhookUrl, "scope", "invitee.created"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = CalendlyInviteeCreatedTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutputParameters, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
