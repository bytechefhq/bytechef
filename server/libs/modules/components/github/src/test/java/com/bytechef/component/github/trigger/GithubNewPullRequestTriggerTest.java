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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.github.util.GithubUtils;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class GithubNewPullRequestTriggerTest extends AbstractGithubTriggerTest {

    @Test
    void testDynamicWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(REPOSITORY))
            .thenReturn("repo");

        githubUtilsMockedStatic.when(
            () -> GithubUtils.subscribeWebhook("repo", "pull_request", webhookUrl, mockedTriggerContext))
            .thenReturn(123);
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = GithubNewPullRequestTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = dynamicWebhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, 123);

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testDynamicWebhookDisable() {
        when(mockedParameters.getRequiredString(REPOSITORY))
            .thenReturn("repo");
        when(mockedParameters.getInteger(ID))
            .thenReturn(123);

        GithubNewPullRequestTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        githubUtilsMockedStatic
            .verify(() -> GithubUtils.unsubscribeWebhook("repo", 123, mockedTriggerContext));
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, Object> content = Map.of("id", 123);

        githubUtilsMockedStatic.when(
            () -> GithubUtils.getContent(mockedWebhookBody))
            .thenReturn(content);

        Map<String, Object> result = GithubNewIssueTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(content, result);
    }
}
