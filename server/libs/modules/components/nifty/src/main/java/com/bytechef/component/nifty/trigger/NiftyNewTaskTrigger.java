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

package com.bytechef.component.nifty.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.nifty.constant.NiftyConstants.APP_ID;
import static com.bytechef.component.nifty.constant.NiftyConstants.ID;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT;

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
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.nifty.util.NiftyUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NiftyNewTaskTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when new task is created.")
        .properties(
            string(APP_ID)
                .label("Application")
                .description("Application to be used for the trigger.")
                .options((OptionsFunction<String>) NiftyUtils::getAppIdOptions)
                .required(true))
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID pod the task."),
                        string(PROJECT)
                            .description("Project under which the task is created."),
                        string("order")
                            .description("Order of the task."),
                        string("milestone")
                            .description("Milestone of the task."))))
        .webhookEnable(NiftyNewTaskTrigger::webhookEnable)
        .webhookDisable(NiftyNewTaskTrigger::webhookDisable)
        .webhookRequest(NiftyNewTaskTrigger::webhookRequest);

    private NiftyNewTaskTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext triggerContext) {

        Map<String, ?> body = triggerContext.http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "endpoint", webhookUrl,
                    "event", List.of("taskCreated"),
                    APP_ID, inputParameters.getRequiredString(APP_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("webhook") instanceof Map<?, ?> map) {
            return new WebhookEnableOutput(
                Map.of(ID, map.get(ID)), null);
        }
        throw new ProviderException("Failed to start Nifty webhook.");

    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        triggerContext.http(http -> http.delete("/webhooks/" + outputParameters.getString(ID)))
            .body(Http.Body.of(Map.of(APP_ID, inputParameters.getRequiredString(APP_ID))))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext triggerContext) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("data");
    }
}
