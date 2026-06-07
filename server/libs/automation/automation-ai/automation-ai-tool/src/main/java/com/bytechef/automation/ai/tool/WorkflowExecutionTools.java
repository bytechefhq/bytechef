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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.ai.tool.exception.WorkflowExecutionToolErrorType;
import com.bytechef.automation.ai.tool.model.TaskExecutionInfo;
import com.bytechef.automation.ai.tool.model.TriggerExecutionInfo;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionDetailInfo;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionSummary;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
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
 * Read-only Spring AI tools for inspecting workflow executions. {@code getWorkflowExecution} resolves the full detail
 * of a single run by id (context-free); {@code listWorkflowExecutions} returns a workspace-scoped summary page, reading
 * the workspace / environment from {@link ToolContext} (see {@link WorkflowExecutionToolContextKeys}).
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowExecutionTools {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionTools.class);

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowExecutionTools(ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade) {
        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
    }

    @Tool(
        description = "Get the full detail of a single workflow execution by id: job status, dates, job-level error, " +
            "the trigger execution, and every task execution with its input, output, and error. Use this to diagnose " +
            "why a specific run behaved the way it did.")
    public WorkflowExecutionDetailInfo getWorkflowExecution(
        @ToolParam(description = "The workflow execution id") long workflowExecutionId, ToolContext toolContext) {

        try {
            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            WorkflowExecutionDTO execution = projectWorkflowExecutionFacade.getWorkflowExecution(workflowExecutionId);

            // Fail-closed workspace ownership check (IDOR guard): the LLM supplies the execution id, so an
            // hallucinated or injected id could otherwise read another workspace's run. Deny when the caller's
            // workspace is unknown or does not own the execution; do not reveal that the id exists elsewhere.
            Project project = execution == null ? null : execution.project();
            Long executionWorkspaceId = project == null ? null : project.getWorkspaceId();

            if (workspaceId == null || executionWorkspaceId == null || !workspaceId.equals(executionWorkspaceId)) {
                throw new ExecutionException(
                    "Workflow execution " + workflowExecutionId + " not found",
                    WorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
            }

            return toDetailInfo(execution);
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception exception) {
            log.error("getWorkflowExecution(): Failed for id={}", workflowExecutionId, exception);

            throw new ExecutionException(
                "Failed to get workflow execution: " + exception.getMessage(), exception,
                WorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
        }
    }

    @Tool(
        description = "List recent workflow executions for the current workspace. Use this to resolve a run the user " +
            "describes (\"the failed run yesterday\", \"the last execution of my onboarding workflow\") to a concrete "
            +
            "execution id. Returns a JSON array of {id, workflowLabel, projectName, status, startDate, endDate}, " +
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
            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            if (workspaceId == null) {
                log.warn("listWorkflowExecutions(): workspace id absent from tool context; returning empty list");

                return List.of();
            }

            Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
            Status status = parseStatus(jobStatus);

            Page<WorkflowExecutionDTO> page = projectWorkflowExecutionFacade.getWorkflowExecutions(
                false, environmentId, status, null, null, null, null, workflowId, workspaceId, 0);

            return page.getContent()
                .stream()
                .map(WorkflowExecutionTools::toSummary)
                .toList();
        } catch (Exception exception) {
            log.error("listWorkflowExecutions(): Failed", exception);

            throw new ExecutionException(
                "Failed to list workflow executions: " + exception.getMessage(), exception,
                WorkflowExecutionToolErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }

    private static WorkflowExecutionDetailInfo toDetailInfo(WorkflowExecutionDTO execution) {
        JobDTO job = execution.job();
        Workflow workflow = execution.workflow();
        Project project = execution.project();

        List<TaskExecutionInfo> taskExecutionInfos = job == null || job.taskExecutions() == null ? List.of()
            : job.taskExecutions()
                .stream()
                .map(WorkflowExecutionTools::toTaskExecutionInfo)
                .toList();

        return new WorkflowExecutionDetailInfo(
            execution.id(),
            job == null ? null : job.workflowId(),
            workflow == null ? null : workflow.getLabel(),
            project == null ? null : project.getName(),
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
        Project project = execution.project();

        return new WorkflowExecutionSummary(
            execution.id(),
            workflow == null ? null : workflow.getLabel(),
            project == null ? null : project.getName(),
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
