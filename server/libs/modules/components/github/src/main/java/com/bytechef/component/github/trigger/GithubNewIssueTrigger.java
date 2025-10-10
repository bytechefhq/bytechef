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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTIES;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY_OUTPUT_PROPERTY;
import static com.bytechef.component.github.util.GithubUtils.getContent;
import static com.bytechef.component.github.util.GithubUtils.subscribeWebhook;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.github.util.GithubUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GithubNewIssueTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newIssue")
        .title("New Issue")
        .description("Triggers when a new issue is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .options((OptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("issue")
                            .properties(ISSUE_OUTPUT_PROPERTIES),
                        object("sender")
                            .description("Information about the author of the issue.")
                            .properties(
                                string("login")
                                    .description("The username of the individual who created the issue."),
                                integer(ID)
                                    .description("ID of the sender.")),
                        string("action")
                            .description(
                                "The action performed on the issue, such as 'opened', 'closed', or 'reopened'."),
                        REPOSITORY_OUTPUT_PROPERTY)))
        .webhookEnable(GithubNewIssueTrigger::webhookEnable)
        .webhookDisable(GithubNewIssueTrigger::webhookDisable)
        .webhookRequest(GithubNewIssueTrigger::webhookRequest);

    private GithubNewIssueTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID, subscribeWebhook(inputParameters.getRequiredString(REPOSITORY), "issues", webhookUrl, context)),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        GithubUtils.unsubscribeWebhook(
            inputParameters.getRequiredString(REPOSITORY), outputParameters.getInteger(ID), context);
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return getContent(body);
    }

}
