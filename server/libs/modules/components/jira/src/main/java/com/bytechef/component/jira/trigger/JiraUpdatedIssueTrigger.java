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

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.UPDATED_ISSUE;

import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import com.bytechef.component.jira.util.JiraUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class JiraUpdatedIssueTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(UPDATED_ISSUE)
        .title("Updated Issue")
        .description("Triggers when an issue is updated.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(PROJECT)
                .label("Project")
                .description("Project where new issue is created.")
                .options((TriggerOptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(ISSUETYPE)
                .label("Issue type")
                .description("The type of issue.")
                .options((TriggerOptionsFunction<String>) JiraOptionsUtils::getIssueTypesIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(false))
        .outputSchema(ISSUE_OUTPUT_PROPERTY)
        .dynamicWebhookEnable(JiraUpdatedIssueTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(JiraUpdatedIssueTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(JiraUpdatedIssueTrigger::dynamicWebhookRequest);

    private JiraUpdatedIssueTrigger() {
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new DynamicWebhookEnableOutput(
            Map.of(ID, JiraUtils.subscribeWebhook(inputParameters, webhookUrl, context, "jira:issue_updated")), null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        JiraUtils.unsubscribeWebhook(outputParameters, context);
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
        TriggerContext context) {

        return body
            .getContent(new TypeReference<Map<String, ?>>() {})
            .get(ISSUE);
    }
}
