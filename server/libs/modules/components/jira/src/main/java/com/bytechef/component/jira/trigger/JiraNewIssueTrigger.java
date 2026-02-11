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

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import com.bytechef.component.jira.util.JiraUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class JiraNewIssueTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newIssue")
        .title("New Issue")
        .description("Triggers when a new issue is created.")
        .help("", "https://docs.bytechef.io/reference/components/jira_v1#new-issue")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where new issue is created.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(ISSUETYPE)
                .label("Issue Type ID")
                .description("ID of the issue type.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueTypesIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(false))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .webhookEnable(JiraNewIssueTrigger::webhookEnable)
        .webhookDisable(JiraNewIssueTrigger::webhookDisable)
        .webhookRequest(JiraNewIssueTrigger::webhookRequest);

    private JiraNewIssueTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID, JiraUtils.subscribeWebhook(inputParameters, webhookUrl, context, "jira:issue_created")), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        JiraUtils.unsubscribeWebhook(outputParameters, context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters output,
        TriggerContext context) {

        return body
            .getContent(new TypeReference<Map<String, ?>>() {})
            .get(ISSUE);
    }
}
