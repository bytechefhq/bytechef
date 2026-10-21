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

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.util.WorkflowPersistCaptureUtils;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class WorkspaceScopedSubAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceScopedSubAgentToolCallback.class);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The task for the specialist, plus any ids or decisions already resolved."
                    },
                    "workspaceId": {
                        "type": "integer",
                        "description": "Target workspace id. Optional when the account has exactly one workspace; otherwise required — an error response lists the candidates."
                    },
                    "environment": {
                        "type": "string",
                        "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT when omitted."
                    }
                },
                "required": ["request"]
            }""";

    private static final String KIND_INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The task for the specialist, plus any ids or decisions already resolved."
                    },
                    "workspaceId": {
                        "type": "integer",
                        "description": "Target workspace id. Optional when the account has exactly one workspace; otherwise required — an error response lists the candidates."
                    },
                    "environment": {
                        "type": "string",
                        "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT when omitted."
                    },
                    "kind": {
                        "type": "string",
                        "enum": [KINDS],
                        "description": "KIND_DESCRIPTION Only DRAFT workflows are edited, and the result says whether every saved workflow fits the kind. Omit it for any other workflow."
                    }
                },
                "required": ["request"]
            }""";

    private static final String API_ENDPOINT_INSTRUCTION =
        "\n\nAPI endpoint requirements: the workflow's trigger is apiPlatform/v1/newApiRequest. Its required " +
            "request parameter is an object whose headers, parameters and body entries are each a string holding a " +
            "JSON Schema of that part of the request (an empty object when the request carries nothing); its " +
            "response parameter is an object whose success, invalidInput, internalError and forbidden entries are " +
            "each a string holding a JSON Schema of that response case. The workflow's last task is " +
            "apiPlatform/v1/responseToAPIRequest with a responseType parameter naming the response case it sends: " +
            "SUCCESS, INVALID_INPUT, INTERNAL_ERROR or FORBIDDEN, and a response parameter, an object whose entry " +
            "for that case (success, invalidInput, internalError or forbidden) holds the value sent back as the JSON " +
            "body, matching that case's schema. Edit only workflows of the project's DRAFT version.";

    private static final String APP_CALLABLE_INSTRUCTION =
        "\n\nApp-callable requirements: the workflow's trigger is workflow/v1/newWorkflowCall with an inputSchema " +
            "parameter that is a string holding a JSON Schema that describes every value the caller supplies. The " +
            "workflow ends with a workflow/v1/responseToWorkflowCall task carrying an outputSchema parameter that " +
            "is a string holding a JSON Schema of the result. Edit only workflows of the project's DRAFT version.";

    private static final String SCHEDULED_INSTRUCTION =
        "\n\nScheduled requirements: the workflow's trigger is one of schedule/v1/cron, schedule/v1/everyDay, " +
            "schedule/v1/everyWeek, schedule/v1/everyMonth or schedule/v1/interval. The workflow writes its results " +
            "with a dataTable/v1/createRecords or dataTable/v1/updateRecord task whose table parameter is the data " +
            "table's baseName. Edit only workflows of the project's DRAFT version.";

    private final ToolCallback delegate;
    private final boolean deployProjectRegistered;
    private final JsonMapper jsonMapper = new JsonMapper();
    private final Map<WorkflowKind, WorkflowKindChecker> workflowKindCheckers;
    private final WorkspaceScopeResolver workspaceScopeResolver;

    public WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceScopeResolver workspaceScopeResolver) {
        this(delegate, workspaceScopeResolver, List.of());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedSubAgentToolCallback(
        ToolCallback delegate, WorkspaceScopeResolver workspaceScopeResolver,
        List<WorkflowKindChecker> workflowKindCheckers) {

        this.delegate = delegate;
        this.workflowKindCheckers = toWorkflowKindCheckerMap(workflowKindCheckers);
        this.deployProjectRegistered = this.workflowKindCheckers.containsKey(WorkflowKind.APP_CALLABLE);
        this.workspaceScopeResolver = workspaceScopeResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        String description = delegateToolDefinition.description() +
            " Supply workspaceId when the account has more than one workspace. Supply environment" +
            " (DEVELOPMENT, STAGING, or PRODUCTION) to target something other than DEVELOPMENT.";

        if (!workflowKindCheckers.isEmpty()) {
            description += " Set kind to build a workflow of that kind.";
        }

        return ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(description)
            .inputSchema(workflowKindCheckers.isEmpty() ? INPUT_SCHEMA : getKindInputSchema())
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            WorkspaceScopedInput input = jsonMapper.readValue(toolInput, WorkspaceScopedInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return ToolErrors.toolError(jsonMapper, "request is required and must not be blank");
            }

            WorkflowKindChecker workflowKindChecker = null;

            if (input.kind() != null) {
                workflowKindChecker = fetchWorkflowKindChecker(input.kind());

                if (workflowKindChecker == null) {
                    return ToolErrors.toolError(
                        jsonMapper, "Unknown kind '" + input.kind() + "'. Supported: " + getSupportedKinds());
                }
            }

            WorkspaceScopeResolver.WorkspaceScope workspaceScope =
                workspaceScopeResolver.resolve(input.workspaceId(), input.environment());

            if (workspaceScope instanceof WorkspaceScopeResolver.Rejected rejected) {
                return rejected.response();
            }

            WorkspaceScopeResolver.Resolved resolved = (WorkspaceScopeResolver.Resolved) workspaceScope;

            Map<String, Object> forwardedContext = resolved.toForwardedContext(toolContext);

            if (workflowKindChecker != null) {
                forwardedContext = WorkflowPersistCaptureUtils.withCaptureHolder(forwardedContext);

                forwardedContext.put(AppCallableDraftGuard.APP_CALLABLE_KEY, true);
            }

            String delegateInput = jsonMapper.writeValueAsString(
                Map.of(
                    "request",
                    workflowKindChecker == null ? request : request + getInstruction(workflowKindChecker.kind())));

            String result = delegate.call(delegateInput, new ToolContext(forwardedContext));

            if (workflowKindChecker == null) {
                return result;
            }

            return checkKind(
                result, WorkflowPersistCaptureUtils.getCapturedWorkflowIds(forwardedContext), workflowKindChecker);
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedSubAgentToolCallback.class, toolDefinition.name(), exception);
        }
    }

    private String checkKind(String result, List<String> workflowIds, WorkflowKindChecker workflowKindChecker) {
        if (workflowIds.isEmpty()) {
            return result;
        }

        WorkflowKind workflowKind = workflowKindChecker.kind();
        List<String> failures = new ArrayList<>();

        for (String workflowId : workflowIds) {
            CallableWorkflowCheck callableWorkflowCheck;

            try {
                callableWorkflowCheck = workflowKindChecker.check(workflowId);
            } catch (RuntimeException exception) {
                log.warn("Checking whether workflow {} is of kind {} failed", workflowId, workflowKind, exception);

                failures.add("workflowId " + workflowId + ": could not be checked");

                continue;
            }

            if (!callableWorkflowCheck.callable()) {
                failures.add("workflowId " + workflowId + ": " + String.join("; ", callableWorkflowCheck.problems()));
            }
        }

        if (!failures.isEmpty()) {
            return ToolErrors.toolError(
                jsonMapper,
                getFailurePrefix(workflowKind) + " " + String.join(" | ", failures) +
                    ". Call buildWorkflow again with kind " + workflowKind + ", naming the workflowId, to fix it.");
        }

        return switch (workflowKind) {
            case APP_CALLABLE -> result + "\n\nApp-callable: " + String.join(", ", workflowIds) +
                ". Publish the project, then call deployProject.";
            case API_ENDPOINT -> result + "\n\n" + describeApiEndpointWorkflows(workflowIds, workflowKindChecker);
            case SCHEDULED -> result + "\n\n" + describeScheduledWorkflows(workflowIds, workflowKindChecker);
        };
    }

    private static String describeApiEndpointWorkflows(
        List<String> workflowIds, WorkflowKindChecker workflowKindChecker) {

        Map<String, String> workflowUuids = fetchWorkflowUuids(workflowIds, workflowKindChecker);

        return (workflowIds.size() == 1 ? "API endpoint workflow: " : "API endpoint workflows: ") +
            describeWorkflows(workflowIds, workflowUuids) +
            ". Publish the project, then createAppApiCollection / createAppApiEndpoint.";
    }

    private String describeScheduledWorkflows(List<String> workflowIds, WorkflowKindChecker workflowKindChecker) {
        Map<String, String> workflowUuids = fetchWorkflowUuids(workflowIds, workflowKindChecker);

        String nextStep;

        if (!deployProjectRegistered) {
            nextStep = "deploy it in the ByteChef UI with " +
                (workflowIds.size() == 1 ? "this workflow enabled." : "these workflows enabled.");
        } else if (workflowUuids.isEmpty()) {
            nextStep = "call deployProject with " +
                (workflowIds.size() == 1 ? "the workflow enabled." : "the workflows enabled.");
        } else {
            nextStep = "call deployProject with " +
                (workflowUuids.size() == 1 ? "this workflow uuid" : "these workflow uuids") + " in enabledWorkflows.";
        }

        return (workflowIds.size() == 1 ? "Scheduled workflow: " : "Scheduled workflows: ") +
            describeWorkflows(workflowIds, workflowUuids) + ". Publish the project, then " + nextStep;
    }

    private static String describeWorkflows(List<String> workflowIds, Map<String, String> workflowUuids) {
        return workflowIds.stream()
            .map(workflowId -> workflowUuids.containsKey(workflowId)
                ? workflowId + " (uuid " + workflowUuids.get(workflowId) + ")" : workflowId)
            .collect(Collectors.joining(", "));
    }

    private static Map<String, String> fetchWorkflowUuids(
        List<String> workflowIds, WorkflowKindChecker workflowKindChecker) {

        Map<String, String> workflowUuids = new LinkedHashMap<>();

        for (String workflowId : workflowIds) {
            try {
                workflowKindChecker.fetchWorkflowUuid(workflowId)
                    .ifPresent(workflowUuid -> workflowUuids.put(workflowId, workflowUuid));
            } catch (RuntimeException exception) {
                log.warn("Fetching the uuid of workflow {} failed", workflowId, exception);
            }
        }

        return workflowUuids;
    }

    private static String getFailurePrefix(WorkflowKind workflowKind) {
        return switch (workflowKind) {
            case APP_CALLABLE -> "The workflow was saved but an app cannot call it.";
            case API_ENDPOINT -> "The workflow was saved but it is not an API endpoint workflow.";
            case SCHEDULED -> "The workflow was saved but it is not a scheduled workflow.";
        };
    }

    private static String getInstruction(WorkflowKind workflowKind) {
        return switch (workflowKind) {
            case APP_CALLABLE -> APP_CALLABLE_INSTRUCTION;
            case API_ENDPOINT -> API_ENDPOINT_INSTRUCTION;
            case SCHEDULED -> SCHEDULED_INSTRUCTION;
        };
    }

    private static String getKindDescription(WorkflowKind workflowKind) {
        return switch (workflowKind) {
            case APP_CALLABLE -> "APP_CALLABLE: an app calls it (workflow/v1/newWorkflowCall trigger, closing " +
                "workflow/v1/responseToWorkflowCall task).";
            case API_ENDPOINT -> "API_ENDPOINT: it serves an API Platform endpoint (apiPlatform/v1/newApiRequest " +
                "trigger, closing apiPlatform/v1/responseToAPIRequest task).";
            case SCHEDULED -> "SCHEDULED: it runs on a schedule trigger and writes its results to a data table.";
        };
    }

    private static Map<WorkflowKind, WorkflowKindChecker> toWorkflowKindCheckerMap(
        List<WorkflowKindChecker> workflowKindCheckers) {

        Map<WorkflowKind, WorkflowKindChecker> workflowKindCheckerMap = new EnumMap<>(WorkflowKind.class);

        for (WorkflowKindChecker workflowKindChecker : workflowKindCheckers) {
            WorkflowKindChecker existingWorkflowKindChecker = workflowKindCheckerMap.putIfAbsent(
                workflowKindChecker.kind(), workflowKindChecker);

            if (existingWorkflowKindChecker != null) {
                log.warn(
                    "Workflow kind {} is claimed by both {} and {}; using {}", workflowKindChecker.kind(),
                    existingWorkflowKindChecker.getClass()
                        .getName(),
                    workflowKindChecker.getClass()
                        .getName(),
                    existingWorkflowKindChecker.getClass()
                        .getName());
            }
        }

        return Collections.unmodifiableMap(workflowKindCheckerMap);
    }

    private @Nullable WorkflowKindChecker fetchWorkflowKindChecker(String kind) {
        for (Map.Entry<WorkflowKind, WorkflowKindChecker> entry : workflowKindCheckers.entrySet()) {
            WorkflowKind workflowKind = entry.getKey();

            if (workflowKind.name()
                .equalsIgnoreCase(kind.trim())) {

                return entry.getValue();
            }
        }

        return null;
    }

    private String getKindInputSchema() {
        String kinds = workflowKindCheckers.keySet()
            .stream()
            .map(workflowKind -> "\"" + workflowKind + "\"")
            .collect(Collectors.joining(", "));

        String description = "The kind of workflow to build; the specialist gets that kind's requirements. " +
            workflowKindCheckers.keySet()
                .stream()
                .map(WorkspaceScopedSubAgentToolCallback::getKindDescription)
                .collect(Collectors.joining(" "));

        return KIND_INPUT_SCHEMA.replace("KINDS", kinds)
            .replace("KIND_DESCRIPTION", description);
    }

    private String getSupportedKinds() {
        return workflowKindCheckers.keySet()
            .stream()
            .map(WorkflowKind::name)
            .collect(Collectors.joining(", "));
    }

    public record WorkspaceScopedInput(
        @Nullable String request, @Nullable Long workspaceId, @Nullable String environment,
        @Nullable String kind) {
    }
}
