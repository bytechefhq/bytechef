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

package com.bytechef.component.linear.trigger;

import static com.bytechef.component.linear.constant.LinearConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.linear.util.LinearUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * @author Marija Horvat
 */
class LinearRemovedIssueTriggerTest extends AbstractLinearTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        WebhookEnableOutput expectedOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        linearUtilsMockedStatic.when(
            () -> LinearUtils.createWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(),
                parametersArgumentCaptor.capture()))
            .thenReturn(expectedOutput);

        WebhookEnableOutput actualOutput = LinearRemovedIssueTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        assertEquals(expectedOutput, actualOutput);
        assertEquals(webhookUrl, stringArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
        assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));

        linearUtilsMockedStatic.when(
            () -> LinearUtils.deleteWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
            .thenAnswer((Answer<Void>) invocation -> null);

        LinearRemovedIssueTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        assertEquals("123", stringArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        linearUtilsMockedStatic.when(
            () -> LinearUtils.executeIssueTriggerQuery(
                stringArgumentCaptor.capture(), webhookBodyArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn(Map.of(ID, "123"));

        Object result = LinearRemovedIssueTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(Map.of(ID, "123"), result);
        assertEquals("remove", stringArgumentCaptor.getValue());
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }
}
