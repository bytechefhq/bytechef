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

package com.bytechef.component.resend.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.resend.constant.ResendConstants.ID;

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
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ResendEmailDeliveredTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("emailDelivered")
        .title("Email Delivered")
        .description("Triggers whenever Resend successfully delivers the email to the recipient's mail server.")
        .help("", "https://docs.bytechef.io/reference/components/resend_v1#email-opened")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                string("created_at")
                                    .description("ISO 8601 timestamp when the email was created."),
                                string("email_id")
                                    .description("ID of the email."),
                                string("from")
                                    .description(
                                        "Sender email address and name in the format \"Name <email@domain.com>\"."),
                                string("subject")
                                    .description("Email subject line."),
                                array("to")
                                    .description("Array of impacted recipient email addresses.")
                                    .items(string())),
                        string("created_at")
                            .description("ISO 8601 timestamp when the webhook event was created."),
                        string("type")
                            .description("The event type that triggered the webhook."))))
        .webhookEnable(ResendEmailDeliveredTrigger::webhookEnable)
        .webhookDisable(ResendEmailDeliveredTrigger::webhookDisable)
        .webhookRequest(ResendEmailDeliveredTrigger::webhookRequest);

    private ResendEmailDeliveredTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        Map<String, ?> body = triggerContext.http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "endpoint", webhookUrl,
                    "events", List.of("email.delivered")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (String) body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/webhooks/" + outputParameters.getString(ID)))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext triggerContext) {

        return body.getContent();
    }
}
