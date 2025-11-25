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
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("The ID of the team.")
                .required(true))
        .output()
        .webhookEnable(FigmaNewCommentTrigger::webhookEnable)
        .webhookDisable(FigmaNewCommentTrigger::webhookDisable)
        .webhookRequest(FigmaNewCommentTrigger::webhookRequest);

    private FigmaNewCommentTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        Map<String, ?> body = triggerContext.http(http -> http.post("/v2/webhooks"))
            .body(Http.Body.of(
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
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext triggerContext) {

        return body.getContent();
    }
}
