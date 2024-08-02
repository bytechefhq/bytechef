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

package com.bytechef.component.jira.trigger;

import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.jira.util.JiraUtils;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class JiraUpdatedIssueTriggerTest extends AbstractJiraTriggerTest {

    @Test
    void testDynamicWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        jiraUtilsMockedStatic.when(
            () -> JiraUtils.subscribeWebhook(mockedParameters, webhookUrl, mockedTriggerContext, "jira:issue_updated"))
            .thenReturn(123);

        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = JiraUpdatedIssueTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = dynamicWebhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, 123);

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, ?> issueMap = Map.of(ISSUE, mockedObject);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(issueMap);

        Object result = JiraUpdatedIssueTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
