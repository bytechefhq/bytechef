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

package com.bytechef.component.jira.trigger;

import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.jira.util.JiraUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class JiraNewIssueTriggerTest extends AbstractJiraTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        jiraUtilsMockedStatic.when(
            () -> JiraUtils.subscribeWebhook(
                parametersArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(123);

        WebhookEnableOutput webhookEnableOutput = JiraNewIssueTrigger.webhookEnable(
            mockedParameters, null, webhookUrl, null, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, 123), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
        assertEquals(List.of(webhookUrl, "jira:issue_created"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testWebhookDisable() {
        JiraNewIssueTrigger.webhookDisable(
            null, null, mockedParameters, null, mockedTriggerContext);

        jiraUtilsMockedStatic
            .verify(() -> JiraUtils.unsubscribeWebhook(mockedParameters, mockedTriggerContext));
    }

    @Test
    void testWebhookRequest() {
        Map<String, ?> issueMap = Map.of(ISSUE, mockedObject);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(issueMap);

        Object result = JiraNewIssueTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(mockedObject, result);
    }
}
