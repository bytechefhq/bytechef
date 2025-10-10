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

package com.bytechef.component.gitlab.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.gitlab.constant.GitlabConstants.ID;
import static com.bytechef.component.gitlab.constant.GitlabConstants.PROJECT_ID;

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
import com.bytechef.component.gitlab.util.GitlabUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class GitlabNewIssueTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newIssue")
        .title("New Issue")
        .description("Triggers when a new issue is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(PROJECT_ID)
                .label("Project")
                .options((OptionsFunction<String>) GitlabUtils::getProjectIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("description")
                            .description("The description of the issue."),
                        integer(ID)
                            .description("The ID of the issue."),
                        integer("iid")
                            .description("The internal ID of the issue."),
                        integer(PROJECT_ID)
                            .description("The ID of the project."),
                        string("title")
                            .description("The title of the issue."))))
        .webhookEnable(GitlabNewIssueTrigger::dynamicWebhookEnable)
        .webhookDisable(GitlabNewIssueTrigger::dynamicWebhookDisable)
        .webhookRequest(GitlabNewIssueTrigger::dynamicWebhookRequest);

    private GitlabNewIssueTrigger() {
    }

    protected static WebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        Map<String, Object> body = context
            .http(http -> http.post("/projects/" + inputParameters.getRequiredString(PROJECT_ID) + "/hooks"))
            .body(
                Http.Body.of(
                    "url", webhookUrl,
                    "issues_events", true,
                    "push_events", false))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (Integer) body.get(ID)), null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context
            .http(http -> http.delete(
                "/projects/" + inputParameters.getRequiredString(PROJECT_ID) + "/hooks/" +
                    outputParameters.getInteger(ID)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute();
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        if (content.get("object_attributes") instanceof Map<?, ?> map && Objects.equals(map.get("action"), "open")) {
            return map;
        }

        return Collections.emptyMap();

    }

}
