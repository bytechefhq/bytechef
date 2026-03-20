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
            string(WORKSPACE)
                .label("Workspace")
                .description("The workspace where the project is located.")
                .required(true)
                .options((OptionsFunction<String>) AsanaUtils::getWorkspaceOptions),
            string(RESOURCE)
                .label("Project")
                .description("The project to monitor for newly created tasks.")
                .options((OptionsFunction<String>) AsanaUtils::getProjectsOptions)
                .optionsLookupDependsOn(WORKSPACE)
                .required(true))
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(GID)
                            .description("Globally unique identifier of the task."),
                        string("name")
                            .description("Name of the task."),
                        string("resource_type")
                            .description("Type of the resource (task)."),
                        string("created_at")
                            .description("Timestamp when the task was created."),
                        string("modified_at")
                            .description("Timestamp when the task was last modified."),
                        string("completed")
                            .description("Indicates whether the task is completed."),
                        string("notes")
                            .description("Description or notes of the task."),
                        object("assignee")
                            .description("User assigned to the task.")
                            .properties(
                                string(GID),
                                string("name")),
                        object("project")
                            .description("Project the task belongs to.")
                            .properties(
                                string(GID),
                                string("name")))))
        .webhookEnable(AsanaNewTaskTrigger::webhookEnable)
        .webhookValidateOnEnable(AsanaNewTaskTrigger::webhookValidateOnEnable)
        .webhookDisable(AsanaNewTaskTrigger::webhookDisable)
        .webhookRequest(AsanaNewTaskTrigger::webhookRequest);

    private AsanaNewTaskTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        String resource = inputParameters.getRequiredString(RESOURCE);

        Map<String, ?> newWebhook = context.http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "data",
                    Map.of(
                        RESOURCE, resource,
                        TARGET, webhookUrl,
                        "filters", List.of(Map.of("action", "added", "resource_type", "task")))))
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

        Map<String, List<Map<String, ?>>> payload = body.getContent(new TypeReference<>() {});

        List<Map<String, ?>> events = payload.get("events");

        if (events.isEmpty()) {
            context.log(log -> log.debug("A heartbeat webhook to verify that the endpoint is still available."));

            if (context.isEditorEnvironment()) {
                throw new ProviderException("Heartbeat webhook received.");
            }

            return null;
        } else {
            for (Map<String, ?> event : events) {
                if (event.get("parent") instanceof Map<?, ?> parent) {
                    Object resourceType = parent.get("resource_type");

                    if (resourceType.equals("project") && event.get("resource") instanceof Map<?, ?> resource) {
                        String taskGid = (String) resource.get("gid");

                        return context.http(http -> http.get("/tasks/%s".formatted(taskGid)))
                            .configuration(Http.responseType(Http.ResponseType.JSON))
                            .execute()
                            .getBody();
                    }
                }
            }
        }

        throw new ProviderException("Asana webhook received an unexpected event.");
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        String hookSecret = headers.firstValue("X-Hook-Secret")
            .orElse(null);

        if (hookSecret != null) {
            return new WebhookValidateResponse(
                null, Map.of("X-Hook-Secret", List.of(hookSecret)), HttpStatus.OK.getValue());
        }

        return WebhookValidateResponse.badRequest();
    }
}
