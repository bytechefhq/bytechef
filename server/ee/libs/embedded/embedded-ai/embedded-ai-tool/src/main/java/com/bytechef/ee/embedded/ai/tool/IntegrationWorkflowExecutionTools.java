/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.ai.tool.WorkflowExecutionToolContextKeys;
import com.bytechef.ee.embedded.ai.tool.exception.IntegrationWorkflowExecutionToolErrorType;
import com.bytechef.ee.embedded.ai.tool.model.TaskExecutionInfo;
import com.bytechef.ee.embedded.ai.tool.model.TriggerExecutionInfo;
import com.bytechef.ee.embedded.ai.tool.model.WorkflowExecutionDetailInfo;
import com.bytechef.ee.embedded.ai.tool.model.WorkflowExecutionSummary;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationWorkflowExecutionTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWorkflowExecutionTools.class);

    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowExecutionTools(IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade) {
        this.integrationWorkflowExecutionFacade = integrationWorkflowExecutionFacade;
    }

    @Tool(
        description = "Get the full detail of a single integration workflow execution by id: job status, dates, " +
            "job-level error, the trigger execution, and every task execution with its input, output, and error. " +
            "Use this to diagnose why a specific run behaved the way it did. When the chat is bound to an " +
            "environment, only that environment's executions can be read.")
    public WorkflowExecutionDetailInfo getWorkflowExecution(
        @ToolParam(description = "The workflow execution id") long workflowExecutionId, ToolContext toolContext) {

        try {
            Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);

            WorkflowExecutionDTO workflowExecution =
                integrationWorkflowExecutionFacade.getWorkflowExecution(workflowExecutionId);

            if (environmentId != null) {
                IntegrationInstanceConfiguration integrationInstanceConfiguration =
                    workflowExecution.integrationInstanceConfiguration();

                Long executionEnvironmentId = integrationInstanceConfiguration == null
                    ? null : integrationInstanceConfiguration.getEnvironmentId();

                if (executionEnvironmentId == null || !environmentId.equals(executionEnvironmentId)) {
                    throw new ExecutionException(
                        "Workflow execution " + workflowExecutionId + " not found",
                        IntegrationWorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
                }
            }

            return toDetailInfo(workflowExecution);
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "getWorkflowExecution(" + workflowExecutionId + ")", "Failed to get workflow execution",
                exception, IntegrationWorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
        }
    }

    @Tool(
        description = "List recent integration workflow executions for the current tenant. Use this to resolve a " +
            "run the user describes (\"the failed run yesterday\", \"the last execution of my onboarding " +
            "workflow\") to a concrete execution id. Returns a JSON array of {id, workflowLabel, integrationName, " +
            "status, startDate, endDate}, newest first. Optional filters: jobStatus narrows to a single status " +
            "(any value outside the listed statuses is an error, never a silently unfiltered listing); workflowId " +
            "filters to one workflow. When the chat is bound to an environment, only that environment's " +
            "executions are listed.")
    public List<WorkflowExecutionSummary> listWorkflowExecutions(
        @ToolParam(
            required = false,
            description = "Optional job status filter; exactly one of CREATED, STARTED, COMPLETED, FAILED, STOPPED, PAYMENT_REQUIRED") @Nullable String jobStatus,
        @ToolParam(
            required = false,
            description = "Optional workflow id to filter a specific workflow's history") @Nullable String workflowId,
        ToolContext toolContext) {

        try {
            Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
            Status status = parseStatus(jobStatus);

            Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
                environmentId, status, null, null, null, null, null, workflowId, 0);

            return page.getContent()
                .stream()
                .map(IntegrationWorkflowExecutionTools::toSummary)
                .toList();
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "listWorkflowExecutions()", "Failed to list workflow executions", exception,
                IntegrationWorkflowExecutionToolErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }

    private static WorkflowExecutionDetailInfo toDetailInfo(WorkflowExecutionDTO workflowExecution) {
        JobDTO job = workflowExecution.job();
        Workflow workflow = workflowExecution.workflow();
        Integration integration = workflowExecution.integration();

        List<TaskExecutionInfo> taskExecutionInfos = job == null || job.taskExecutions() == null ? List.of()
            : job.taskExecutions()
                .stream()
                .map(IntegrationWorkflowExecutionTools::toTaskExecutionInfo)
                .toList();

        return new WorkflowExecutionDetailInfo(
            workflowExecution.id(),
            job == null ? null : job.workflowId(),
            workflow == null ? null : workflow.getLabel(),
            integration == null ? null : integration.getName(),
            job == null || job.status() == null ? null : job.status()
                .name(),
            job == null ? null : job.startDate(),
            job == null ? null : job.endDate(),
            job == null ? null : errorMessage(job.error()),
            toTriggerExecutionInfo(workflowExecution.triggerExecution()),
            taskExecutionInfos);
    }

    private static TaskExecutionInfo toTaskExecutionInfo(TaskExecutionDTO taskExecution) {
        return new TaskExecutionInfo(
            taskExecution.title() != null ? taskExecution.title() : taskExecution.type(),
            taskExecution.type(),
            taskExecution.status() == null ? null : taskExecution.status()
                .name(),
            taskExecution.input(),
            taskExecution.output(),
            errorMessage(taskExecution.error()));
    }

    private static @Nullable TriggerExecutionInfo
        toTriggerExecutionInfo(@Nullable TriggerExecutionDTO triggerExecution) {

        if (triggerExecution == null) {
            return null;
        }

        return new TriggerExecutionInfo(
            triggerExecution.type(),
            triggerExecution.status() == null ? null : triggerExecution.status()
                .name(),
            triggerExecution.output(),
            errorMessage(triggerExecution.error()));
    }

    private static WorkflowExecutionSummary toSummary(WorkflowExecutionDTO workflowExecution) {
        JobDTO job = workflowExecution.job();
        Workflow workflow = workflowExecution.workflow();
        Integration integration = workflowExecution.integration();

        return new WorkflowExecutionSummary(
            workflowExecution.id(),
            workflow == null ? null : workflow.getLabel(),
            integration == null ? null : integration.getName(),
            job == null || job.status() == null ? null : job.status()
                .name(),
            job == null ? null : job.startDate(),
            job == null ? null : job.endDate());
    }

    private static @Nullable String errorMessage(@Nullable ExecutionError error) {
        return error == null ? null : error.getMessage();
    }

    private static @Nullable Long asLong(@Nullable ToolContext toolContext, String key) {
        Object value = toolContext == null ? null : toolContext.getContext()
            .get(key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable Status parseStatus(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ExecutionException(
                "Unknown jobStatus '" + value + "'. Valid values: " + Arrays.stream(Status.values())
                    .map(Status::name)
                    .collect(Collectors.joining(", ")),
                IntegrationWorkflowExecutionToolErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }
}
