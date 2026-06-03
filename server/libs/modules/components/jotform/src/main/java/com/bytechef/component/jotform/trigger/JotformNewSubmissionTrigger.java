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

package com.bytechef.component.jotform.trigger;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

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
import com.bytechef.component.jotform.util.JotformUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class JotformNewSubmissionTrigger {

    public static final String FORM_ID = "formId";
    public static final String WEBHOOK_URL = "webhookURL";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newSubmission")
        .title("New Submission")
        .description("Triggers when someone submits a response to a form.")
        .help("", "https://docs.bytechef.io/reference/components/jotform_v1#new-submission")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(FORM_ID)
                .label("Form ID")
                .description("The ID of the form to watch for new submissions.")
                .options((OptionsFunction<String>) JotformUtils::getFormIdOptions)
                .required(true))
        .output()
        .webhookDisable(JotformNewSubmissionTrigger::webhookDisable)
        .webhookEnable(JotformNewSubmissionTrigger::webhookEnable)
        .webhookRequest(JotformNewSubmissionTrigger::webhookRequest);

    private JotformNewSubmissionTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        Map<String, Object> body = triggerContext
            .http(http -> http.get("/form/%s/webhooks".formatted(inputParameters.getRequiredString(FORM_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("content") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();

                if (value.equals(outputParameters.getRequiredString(WEBHOOK_URL))) {
                    triggerContext
                        .http(http -> http.delete(
                            "/form/%s/webhooks/%s".formatted(inputParameters.getRequiredString(FORM_ID),
                                entry.getKey())))
                        .execute();

                    break;
                }
            }
        }
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        triggerContext
            .http(http -> http.post("/form/%s/webhooks".formatted(inputParameters.getRequiredString(FORM_ID))))
            .header("Content-Type", "multipart/form-data")
            .body(Http.Body.of(Map.of(WEBHOOK_URL, webhookUrl), Http.BodyContentType.FORM_DATA))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return new WebhookEnableOutput(Map.of(WEBHOOK_URL, webhookUrl), null);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters webhookEnableOutputParameters,
        TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        Object o = content.get("rawRequest");

        if (o instanceof String s) {
            content.put("parsedRawRequest", context.json(json -> json.read(s)));
        }

        return content;
    }
}
