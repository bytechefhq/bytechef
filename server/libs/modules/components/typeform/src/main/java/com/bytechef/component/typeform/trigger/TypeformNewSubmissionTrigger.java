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

package com.bytechef.component.typeform.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.typeform.constant.TypeformConstants.ID;

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
import com.bytechef.component.typeform.util.TypeformUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Monika Kušter
 */
public class TypeformNewSubmissionTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newSubmission")
        .title("New Submission")
        .description("Triggers when form is submitted.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string("form")
                .label("Form Name")
                .options((OptionsFunction<String>) TypeformUtils::getFormOptions)
                .required(true))
        .output(
            outputSchema(
                // TODO
                object()))
        .webhookEnable(TypeformNewSubmissionTrigger::webhookEnable)
        .webhookDisable(TypeformNewSubmissionTrigger::webhookDisable)
        .webhookRequest(TypeformNewSubmissionTrigger::webhookRequest);

    private TypeformNewSubmissionTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        UUID uuid = UUID.randomUUID();

        context.http(http -> http.put("/forms/" + inputParameters.getRequiredString("form") + "/webhooks/" + uuid))
            .body(
                Http.Body.of(
                    "enabled", true,
                    "url", webhookUrl,
                    "event_types", List.of(Map.of("form_response", true))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return new WebhookEnableOutput(Map.of(ID, uuid.toString()), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(
            http -> http.delete(
                "/forms/" + inputParameters.getRequiredString("form") + "/webhooks/" +
                    outputParameters.getRequiredString(ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        // TODO

        return null;
    }
}
