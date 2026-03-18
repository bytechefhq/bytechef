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

package com.bytechef.component.asana.trigger;

import static com.bytechef.component.asana.constant.AsanaConstants.GID;
import static com.bytechef.component.asana.constant.AsanaConstants.RESOURCE;
import static com.bytechef.component.asana.constant.AsanaConstants.TARGET;
import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.HttpStatus;
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
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class AsanaNewTaskTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when new task is created.")
        .properties(
            object("data")
                .properties(
                    string(WORKSPACE)
                        .label("Workspace")
                        .description("The workspace where the project is located.")
                        .required(true)
                        .options((OptionsFunction<String>) AsanaUtils::getWorkspaceOptions)),
            string(RESOURCE)
                .label("Project")
                .description("The project to monitor for newly created tasks.")
                .options((OptionsFunction<String>) AsanaUtils::getProjectOptions)
                .optionsLookupDependsOn("data.workspace")
                .required(true))
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(GID)
                            .label("Task ID")
                            .description("Globally unique identifier of the task."),
                        string("name")
                            .label("Name")
                            .description("Name of the task."),
                        string("resource_type")
                            .label("Resource Type")
                            .description("Type of the resource (task)."),
                        string("created_at")
                            .label("Created At")
                            .description("Timestamp when the task was created."),
                        string("modified_at")
                            .label("Modified At")
                            .description("Timestamp when the task was last modified."),
                        string("completed")
                            .label("Completed")
                            .description("Indicates whether the task is completed."),
                        string("notes")
                            .label("Notes")
                            .description("Description or notes of the task."),
                        object("assignee")
                            .label("Assignee")
                            .description("User assigned to the task.")
                            .properties(
                                string(GID).label("User ID"),
                                string("name").label("Name")),
                        object("project")
                            .label("Project")
                            .description("Project the task belongs to.")
                            .properties(
                                string(GID).label("Project ID"),
                                string("name").label("Name")))))
        .webhookEnable(AsanaNewTaskTrigger::webhookEnable)
        .webhookValidateOnEnable(AsanaNewTaskTrigger::webhookValidateOnEnable)
        .webhookDisable(AsanaNewTaskTrigger::webhookDisable)
        .webhookRequest(AsanaNewTaskTrigger::webhookRequest);

    private AsanaNewTaskTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        String resource = inputParameters.getRequiredString(RESOURCE);

        Map<String, ?> newWebhook = context.http(http -> http.post("/webhooks"))
            .body(Http.Body.of("data",
                Map.of(RESOURCE, resource, TARGET, webhookUrl, "filters",
                    List.of(Map.of("action", "added", "resource_type", "task")))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
        if (newWebhook.get("data") instanceof Map<?, ?> map) {
            return new WebhookEnableOutput(Map.of(GID, map.get("gid")), null);
        }
        throw new ProviderException("Failed to start Asana webhook.");
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        triggerContext.http(http -> http.delete("/webhooks/" + outputParameters.getString(GID)))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> payload = body.getContent(new TypeReference<>() {});

        Object eventsObj = payload.get("events");

        if (eventsObj instanceof List<?> events && events.isEmpty()) {
            throw new ProviderException("Skipping execution due to empty events");
        }

        return payload;
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        String hookSecret = headers.firstValue("X-Hook-Secret")
            .orElse(null);

        if (hookSecret != null) {
            return new WebhookValidateResponse(
                "",
                Map.of("X-Hook-Secret", List.of(hookSecret)),
                HttpStatus.OK.getValue());
        }

        return WebhookValidateResponse.badRequest();
    }
}
