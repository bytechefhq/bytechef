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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.linear.util.LinearUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class LinearUpdatedIssueTriggerTest extends AbstractLinearTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        WebhookEnableOutput expectedOutput = new WebhookEnableOutput(Map.of("id", "123"), null);

        linearUtilsMockedStatic.when(
            () -> LinearUtils.createWebhook("Issue", webhookUrl, mockedTriggerContext))
            .thenReturn(expectedOutput);

        WebhookEnableOutput actualOutput = LinearUpdatedIssueTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        assertEquals(expectedOutput.parameters(), actualOutput.parameters());
        assertEquals(expectedOutput.webhookExpirationDate(), actualOutput.webhookExpirationDate());
    }

    @Test
    void testWebhookRequest() {
        linearUtilsMockedStatic.when(
            () -> LinearUtils.executeIssueTriggerQuery("update", mockedWebhookBody, mockedTriggerContext))
            .thenReturn(Map.of("id", "123"));

        Object result = LinearUpdatedIssueTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(Map.of("id", "123"), result);
    }
}
