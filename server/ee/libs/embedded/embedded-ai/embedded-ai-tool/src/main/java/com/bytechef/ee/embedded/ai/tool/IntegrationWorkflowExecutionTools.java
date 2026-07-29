/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job.Status;
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
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Read-only Spring AI tools for inspecting embedded integration workflow executions. The embedded mirror of the
 * automation {@code WorkflowExecutionTools}, thin-bound onto {@link IntegrationWorkflowExecutionFacade}.
 *
 * <p>
 * Unlike the automation analog there is no workspace-ownership (IDOR) guard on {@code getWorkflowExecution}: embedded
 * deployments are tenant-scoped (the ambient tenant is the isolation boundary), so there is no per-workspace
 * subdivision to guard against within a tenant. There is, however, an environment-scope guard: when the tool context
 * carries an environment id, {@code getWorkflowExecution} denies reads of executions belonging to a different
 * environment, mirroring the workspace-ownership guard in the automation analog.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class IntegrationWorkflowExecutionTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWorkflowExecutionTools.class);

    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public IntegrationWorkflowExecutionTools(IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade) {
        this.integrationWorkflowExecutionFacade = integrationWorkflowExecutionFacade;
    }

    @Tool(
        description = "Get the full detail of a single workflow execution by id: job status, dates, job-level error, " +
            "the trigger execution, and every task execution with its input, output, and error. Use this to diagnose " +
            "why a specific run behaved the way it did.")
    public WorkflowExecutionDetailInfo getWorkflowExecution(
        @ToolParam(description = "The workflow execution id") long workflowExecutionId, ToolContext toolContext) {

        try {
            Long environmentId = asLong(toolContext, IntegrationWorkflowExecutionToolContextKeys.ENVIRONMENT_ID);

            WorkflowExecutionDTO execution =
                integrationWorkflowExecutionFacade.getWorkflowExecution(workflowExecutionId);

            // Fail-closed environment scope check (IDOR guard): the LLM supplies the execution id, so an
            // hallucinated or injected id could otherwise read another environment's run. Only enforced when the
            // tool context carries an environment id; when absent, the ambient tenant remains the isolation
            // boundary. Do not reveal that the id exists in another environment.
            if (environmentId != null) {
                IntegrationInstanceConfiguration integrationInstanceConfiguration =
                    execution.integrationInstanceConfiguration();
                Long executionEnvironmentId = integrationInstanceConfiguration == null ? null
                    : integrationInstanceConfiguration.getEnvironmentId();

                if (executionEnvironmentId == null || !environmentId.equals(executionEnvironmentId)) {
                    throw new ExecutionException(
                        "Workflow execution " + workflowExecutionId + " not found",
                        IntegrationWorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
                }
            }

            return toDetailInfo(execution);
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception exception) {
            log.error("getWorkflowExecution(): Failed for id={}", workflowExecutionId, exception);

            throw new ExecutionException(
                "Failed to get workflow execution: " + exception.getMessage(), exception,
                IntegrationWorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
        }
    }

    @Tool(
        description = "List recent workflow executions for the current tenant. Use this to resolve a run the user " +
            "describes (\"the failed run yesterday\", \"the last execution of my onboarding workflow\") to a concrete "
            +
            "execution id. Returns a JSON array of {id, workflowLabel, integrationName, status, startDate, endDate}, " +
            "newest first. Optional filters: jobStatus narrows to a single status; workflowId filters to one workflow.")
    public List<WorkflowExecutionSummary> listWorkflowExecutions(
        @ToolParam(
            required = false,
            description = "Optional job status filter (CREATED, STARTED, COMPLETED, STOPPED, FAILED)") @Nullable String jobStatus,
        @ToolParam(
            required = false,
            description = "Optional workflow id to filter a specific workflow's history") @Nullable String workflowId,
        ToolContext toolContext) {

        try {
            Long environmentId = asLong(toolContext, IntegrationWorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
            Status status = parseStatus(jobStatus);

            Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
                environmentId, status, null, null, null, null, workflowId, 0);

            return page.getContent()
                .stream()
                .map(IntegrationWorkflowExecutionTools::toSummary)
                .toList();
        } catch (Exception exception) {
            log.error("listWorkflowExecutions(): Failed", exception);

            throw new ExecutionException(
                "Failed to list workflow executions: " + exception.getMessage(), exception,
                IntegrationWorkflowExecutionToolErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }

    private static WorkflowExecutionDetailInfo toDetailInfo(WorkflowExecutionDTO execution) {
        JobDTO job = execution.job();
        Workflow workflow = execution.workflow();
        Integration integration = execution.integration();

        List<TaskExecutionInfo> taskExecutionInfos = job == null || job.taskExecutions() == null ? List.of()
            : job.taskExecutions()
                .stream()
                .map(IntegrationWorkflowExecutionTools::toTaskExecutionInfo)
                .toList();

        return new WorkflowExecutionDetailInfo(
            execution.id(),
            job == null ? null : job.workflowId(),
            workflow == null ? null : workflow.getLabel(),
            integration == null ? null : integration.getName(),
            job == null || job.status() == null ? null : job.status()
                .name(),
            job == null ? null : job.startDate(),
            job == null ? null : job.endDate(),
            job == null ? null : errorMessage(job.error()),
            toTriggerExecutionInfo(execution.triggerExecution()),
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

    private static WorkflowExecutionSummary toSummary(WorkflowExecutionDTO execution) {
        JobDTO job = execution.job();
        Workflow workflow = execution.workflow();
        Integration integration = execution.integration();

        return new WorkflowExecutionSummary(
            execution.id(),
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

    private static @Nullable Long asLong(ToolContext toolContext, String key) {
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
            return Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}
