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

package com.bytechef.component.bitbucket.trigger;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.ACTIVE;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.EVENTS;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.ID;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.REPOSITORY;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.URL;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.bitbucket.util.BitbucketUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
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
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BitbucketRepositoryPushTrigger {
    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("repositoryPush")
        .title("Repository Push")
        .description("Triggers whenever a repository push occurs.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(WORKSPACE)
                .label("Workspace")
                .description("Workspace where the repository is located.")
                .required(true)
                .options((OptionsFunction<String>) BitbucketUtils::getWorkspaceOptions),
            string(REPOSITORY)
                .label("Repository")
                .description("Repository that will be connected to the trigger.")
                .required(true)
                .options((OptionsFunction<String>) BitbucketUtils::getRepositoryOptions)
                .optionsLookupDependsOn(WORKSPACE))
        .output()
        .webhookEnable(BitbucketRepositoryPushTrigger::webhookEnable)
        .webhookDisable(BitbucketRepositoryPushTrigger::webhookDisable)
        .webhookRequest(BitbucketRepositoryPushTrigger::webhookRequest);

    private BitbucketRepositoryPushTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(
            http -> http.delete("/repositories/%s/%s/hooks/%s".formatted(
                inputParameters.getRequiredString(WORKSPACE),
                inputParameters.getRequiredString(REPOSITORY),
                outputParameters.get(ID)
                    .toString()
                    .replaceAll("[{}]", ""))))
            .execute();
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, Object> body = context.http(http -> http.post("/repositories/%s/%s/hooks".formatted(
            inputParameters.getRequiredString(WORKSPACE),
            inputParameters.getRequiredString(REPOSITORY))))
            .body(
                Http.Body.of(
                    URL, webhookUrl,
                    ACTIVE, true,
                    EVENTS, List.of("repo:push")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String webhookId = (String) body.get("uuid");

        return new WebhookEnableOutput(Map.of(ID, webhookId), null);
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return body.getContent(new TypeReference<>() {});
    }
}
