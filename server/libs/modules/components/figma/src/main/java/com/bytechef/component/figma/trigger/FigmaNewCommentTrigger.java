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

package com.bytechef.component.figma.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.figma.constant.FigmaConstants.ID;
import static com.bytechef.component.figma.constant.FigmaConstants.TEAM_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;
import java.util.UUID;

/**
 * @author Monika Kušter
 */
public class FigmaNewCommentTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newComment")
        .title("New Comment")
        .description("Triggers when new comment is posted.")
        .help("", "https://docs.bytechef.io/reference/components/figma_v1#new-comment")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("The ID of the team.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("file_name")
                            .description("The name of the file that was updated."),
                        string("created_at")
                            .description("The UTC ISO 8601 time at which the comment was left."),
                        string("comment_id")
                            .description("ID of the comment."),
                        object("triggered_by")
                            .description("The user that made the comment and triggered this event.")
                            .properties(
                                string("id")
                                    .description("ID of the user who triggered the event."),
                                string("handle")
                                    .description("Name of the user who triggered the event."),
                                string("email")
                                    .description("Email associated with the user's account."),
                                string("img_url")
                                    .description("URL link to the user's profile image.")),
                        string("file_key")
                            .description("The key of the file that was updated."),
                        integer("retries")
                            .description("Number of times the event has been retried."),
                        string("event_type")
                            .description("Type of the event."),
                        string("webhook_id")
                            .description("The id of the webhook that caused the callback."),
                        string("parent_id")
                            .description("If present, the id of the comment to which this is the reply."),
                        string("resolved_at")
                            .description("If set, the UTC ISO 8601 time the comment was resolved."),
                        array("mentions")
                            .description("Users that were mentioned in the comment.")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the user."),
                                        string("handle")
                                            .description("Name of the user."),
                                        string("email")
                                            .description("Email associated with the user's account."),
                                        string("img_url")
                                            .description("URL link to the user's profile image."))),
                        array("comment")
                            .description("Contents of the comment itself.")
                            .items(
                                object()
                                    .properties(
                                        string("text")
                                            .description("Text of the comment."),
                                        string("mention")
                                            .description(
                                                "User id that is set if a fragment refers to a user mention."))),
                        string("order_id")
                            .description(
                                "Only set for top level comments. The number displayed with the comment in the UI."),
                        string("passcode")
                            .description(
                                "The passcode specified when the webhook was created, should match what was " +
                                    "initially provided."),
                        string("timestamp")
                            .description("UTC ISO 8601 timestamp of when the event was triggered."))))
        .webhookEnable(FigmaNewCommentTrigger::webhookEnable)
        .webhookDisable(FigmaNewCommentTrigger::webhookDisable)
        .webhookRequest(FigmaNewCommentTrigger::webhookRequest);

    private FigmaNewCommentTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        Map<String, ?> body = triggerContext.http(http -> http.post("/v2/webhooks"))
            .body(
                Http.Body.of(
                    "event_type", "FILE_COMMENT",
                    TEAM_ID, inputParameters.getRequiredString(TEAM_ID),
                    "endpoint", webhookUrl,
                    "passcode", UUID.randomUUID()))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (String) body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/v2/webhooks/" + outputParameters.getString(ID)))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext triggerContext) {

        return body.getContent();
    }
}
