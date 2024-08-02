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

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.NEW_PULL_REQUEST;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;
import static com.bytechef.component.github.util.GithubUtils.getContent;
import static com.bytechef.component.github.util.GithubUtils.subscribeWebhook;

import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.github.util.GithubUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GithubNewPullRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_PULL_REQUEST)
        .title("New Pull Request")
        .description("Triggers when a new pull request is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .options((TriggerOptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true))
        .outputSchema(
            object()
                .properties(
                    integer("number"),
                    object("pull_request")
                        .properties(
                            integer(ID),
                            string("state"),
                            string(TITLE),
                            string(BODY),
                            integer("commits")),
                    object("sender")
                        .properties(
                            string("login"),
                            integer(ID)),
                    string("action"),
                    object("repository")
                        .properties(
                            integer(ID),
                            string("name"),
                            string("full_name"),
                            object("owner")
                                .properties(
                                    string("login"),
                                    integer(ID)),
                            string("visibility"),
                            integer("forks"),
                            integer("open_issues"),
                            string("default_branch"))))
        .dynamicWebhookEnable(GithubNewPullRequestTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(GithubNewPullRequestTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(GithubNewPullRequestTrigger::dynamicWebhookRequest);

    private GithubNewPullRequestTrigger() {
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new DynamicWebhookEnableOutput(
            Map.of(ID,
                subscribeWebhook(inputParameters.getRequiredString(REPOSITORY), "pull_request", webhookUrl, context)),
            null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        GithubUtils.unsubscribeWebhook(
            inputParameters.getRequiredString(REPOSITORY), outputParameters.getInteger(ID), context);
    }

    protected static Map<String, Object> dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output, TriggerContext context) {

        return getContent(body);
    }
}
