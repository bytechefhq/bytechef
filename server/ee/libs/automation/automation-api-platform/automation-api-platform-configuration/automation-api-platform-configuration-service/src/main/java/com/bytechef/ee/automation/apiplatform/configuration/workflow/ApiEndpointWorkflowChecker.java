/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.workflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A workflow can back an API Platform endpoint when its trigger is {@code apiPlatform/v1/newApiRequest} with a request
 * of readable schemas and it answers the request with {@code apiPlatform/v1/responseToAPIRequest} tasks that each carry
 * a response.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiEndpointWorkflowChecker implements WorkflowKindChecker {

    static final String NO_REQUEST = "trigger has no request parameter";
    static final String NO_RESPONSE = "responseToAPIRequest task %s has no response parameter";
    static final String NO_RESPONSE_TASK = "no responseToAPIRequest task";
    static final String REQUEST_SCHEMA_UNREADABLE = "request schema unreadable";

    private static final String API_PLATFORM = "apiPlatform";
    private static final String NEW_API_REQUEST = "newApiRequest";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final List<String> REQUEST_SCHEMA_KEYS = List.of("headers", "parameters", "body");
    private static final String RESPONSE_TO_API_REQUEST = "responseToAPIRequest";

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApiEndpointWorkflowChecker(ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowKind kind() {
        return WorkflowKind.API_ENDPOINT;
    }

    @Override
    public CallableWorkflowCheck check(String workflowId) {
        Optional<Workflow> fetchedWorkflow = workflowService.fetchWorkflow(workflowId);

        if (fetchedWorkflow.isEmpty()) {
            return new CallableWorkflowCheck(List.of("workflow %s does not exist".formatted(workflowId)), null);
        }

        Workflow workflow = fetchedWorkflow.get();
        List<String> problems = new ArrayList<>();

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (workflowTriggers.isEmpty()) {
            problems.add("trigger is none, not apiPlatform/v1/newApiRequest");
        } else {
            WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

            if (!isApiPlatformOperation(workflowTrigger.getType(), NEW_API_REQUEST)) {
                problems.add("trigger is %s, not apiPlatform/v1/newApiRequest".formatted(workflowTrigger.getType()));
            } else {
                Map<String, ?> triggerParameters = workflowTrigger.getParameters();

                if (triggerParameters.get(REQUEST) == null) {
                    problems.add(NO_REQUEST);
                } else if (!isRequestReadable(triggerParameters)) {
                    problems.add(REQUEST_SCHEMA_UNREADABLE);
                }
            }
        }

        List<WorkflowTask> enabledWorkflowTasks = WorkflowTaskUtils.getTasks(workflow.getTasks(), null);

        List<WorkflowTask> responseWorkflowTasks = enabledWorkflowTasks.stream()
            .filter(workflowTask -> isApiPlatformOperation(workflowTask.getType(), RESPONSE_TO_API_REQUEST))
            .toList();

        if (responseWorkflowTasks.isEmpty()) {
            problems.add(NO_RESPONSE_TASK);
        }

        for (WorkflowTask responseWorkflowTask : responseWorkflowTasks) {
            Map<String, ?> taskParameters = responseWorkflowTask.getParameters();

            if (!(taskParameters.get(RESPONSE) instanceof Map<?, ?>)) {
                problems.add(NO_RESPONSE.formatted(responseWorkflowTask.getName()));
            }
        }

        return new CallableWorkflowCheck(problems, null);
    }

    @Override
    public Optional<String> fetchWorkflowUuid(String workflowId) {
        return projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId)
            .map(ProjectWorkflow::getUuidAsString);
    }

    private static boolean isApiPlatformOperation(String type, String operation) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return Objects.equals(workflowNodeType.name(), API_PLATFORM) &&
            Objects.equals(workflowNodeType.operation(), operation);
    }

    private static boolean isRequestReadable(Map<String, ?> triggerParameters) {
        Object request = triggerParameters.get(REQUEST);

        if (!(request instanceof Map<?, ?> requestMap)) {
            return false;
        }

        for (String schemaKey : REQUEST_SCHEMA_KEYS) {
            Object schema = requestMap.get(schemaKey);

            if (schema == null || (schema instanceof String string && string.isBlank())) {
                continue;
            }

            if (!(schema instanceof String string) || !isJsonObject(string)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isJsonObject(String schema) {
        try {
            JsonUtils.readMap(schema);

            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
