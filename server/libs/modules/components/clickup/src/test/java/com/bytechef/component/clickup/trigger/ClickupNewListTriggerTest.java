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

package com.bytechef.component.clickup.trigger;

import static com.bytechef.component.clickup.constant.ClickupConstants.ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class ClickupNewListTriggerTest extends AbstractClickupTriggerTest {

    @Test
    void testDynamicWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(WORKSPACE_ID))
            .thenReturn("workspace");

        githubUtilsMockedStatic.when(
            () -> ClickupUtils.subscribeWebhook(webhookUrl, mockedTriggerContext, "workspace", "listCreated"))
            .thenReturn("abc");
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = ClickupNewListTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = dynamicWebhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, "abc");

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testDynamicWebhookDisable() {
        when(mockedParameters.getString(ID))
            .thenReturn("abc");

        ClickupNewListTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        githubUtilsMockedStatic
            .verify(() -> ClickupUtils.unsubscribeWebhook(mockedTriggerContext, "abc"));
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, Object> map = Map.of("key", Map.of(ID, "123"));

        githubUtilsMockedStatic.when(
            () -> ClickupUtils.getCreatedObject(mockedWebhookBody, mockedTriggerContext, "list_id", "/list/"))
            .thenReturn(map);

        Map<String, Object> result = ClickupNewListTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(map, result);
    }
}
