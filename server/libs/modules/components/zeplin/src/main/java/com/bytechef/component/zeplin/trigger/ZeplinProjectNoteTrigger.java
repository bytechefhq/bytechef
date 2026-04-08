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

package com.bytechef.component.zeplin.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.zeplin.constant.ZeplinConstants.ID;
import static com.bytechef.component.zeplin.constant.ZeplinConstants.PROJECT_ID;

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
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.zeplin.util.ZeplinUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Monika Kušter
 */
public class ZeplinProjectNoteTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("projectNote")
        .title("Project Note")
        .description("Triggers when new note is created, deleted or updated in specified project.")
        .help("", "https://docs.bytechef.io/reference/components/zeplin_v1#project-note")
        .properties(
            string(PROJECT_ID)
                .label("Project ID")
                .description("ID of the project you want to monitor.")
                .options((OptionsFunction<String>) ZeplinUtils::getProjectIdOptions)
                .required(true))
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        object("context")
                            .properties(
                                object("project")
                                    .properties(
                                        string(ID)
                                            .description("The ID of the project that triggered the webhook."),
                                        string("name")
                                            .description("The name of the project."))),
                        object("resource")
                            .properties(
                                object("data")
                                    .properties(
                                        string(ID),
                                        string("status"),
                                        array("comments")
                                            .items(
                                                object()
                                                    .properties(
                                                        string(ID)
                                                            .description("The ID of the comment."),
                                                        object("author")
                                                            .properties(
                                                                string(ID)
                                                                    .description("The ID of the author."),
                                                                string("email")
                                                                    .description("The email of the author."),
                                                                string("username")
                                                                    .description("The username of the author.")),
                                                        string("content")
                                                            .description("The content of the comment."))))),
                        string("action")
                            .description("The action that triggered the webhook."),
                        string("event")
                            .description("The event that triggered the webhook."),
                        integer("timestamp")
                            .description("The timestamp of the event."))))
        .webhookEnable(ZeplinProjectNoteTrigger::webhookEnable)
        .webhookDisable(ZeplinProjectNoteTrigger::webhookDisable)
        .webhookRequest(ZeplinProjectNoteTrigger::webhookRequest)
        .webhookValidateOnEnable(ZeplinProjectNoteTrigger::webhookValidate);

    private ZeplinProjectNoteTrigger() {
    }

    public static WebhookValidateResponse webhookValidate(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        return new WebhookValidateResponse(204);
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext triggerContext) {

        Map<String, String> body = triggerContext
            .http(http -> http.post("/projects/" + inputParameters.getRequiredString(PROJECT_ID) + "/webhooks"))
            .body(Http.Body.of(
                "url", webhookUrl,
                "secret", UUID.randomUUID(),
                "events", List.of("project.note")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        triggerContext.http(http -> http
            .delete("/projects/%s/webhooks/%s".formatted(
                inputParameters.getRequiredString(PROJECT_ID), outputParameters.getRequiredString(ID))))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext triggerContext) {

        return body.getContent();
    }
}
