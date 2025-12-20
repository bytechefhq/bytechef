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

package com.bytechef.component.wrike.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.wrike.constant.WrikeConstants.DATA;
import static com.bytechef.component.wrike.constant.WrikeConstants.ID;

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
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class WrikeNewTaskTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when a new task is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("taskId")
                            .description("The task ID."),
                        string("webhookId")
                            .description("The webhook ID."),
                        string("eventAuthorId")
                            .description("The ID of the author of the task."),
                        string("eventType")
                            .description("Event type that happened."),
                        string("lastUpdatedDate")
                            .description("Date of the last update."))))
        .webhookDisable(WrikeNewTaskTrigger::webhookDisable)
        .webhookEnable(WrikeNewTaskTrigger::webhookEnable)
        .webhookRequest(WrikeNewTaskTrigger::webhookRequest);

    private WrikeNewTaskTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/webhooks/%s".formatted(outputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        Map<String, Object> body = context.http(http -> http.post("/webhooks"))
            .queryParameters("hookUrl", webhookUrl, "events", List.of("TaskCreated"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof List<?> webhooks) {
            for (Object webhook : webhooks) {
                if (webhook instanceof Map<?, ?> webhookMap) {
                    return new WebhookEnableOutput(Map.of(ID, (String) webhookMap.get(ID)), null);
                }
            }
        }

        throw new ProviderException("Failed to enable webhook for new task trigger.");
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders httpHeaders,
        HttpParameters httpParameters, WebhookBody webhookBody, WebhookMethod webhookMethod,
        Parameters webhookEnableOutput, TriggerContext triggerContext) {

        return webhookBody.getContent(new TypeReference<>() {});
    }
}
