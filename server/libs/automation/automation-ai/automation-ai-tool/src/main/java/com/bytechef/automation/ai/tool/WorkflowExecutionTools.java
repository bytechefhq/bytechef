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
import com.bytechef.automation.ai.tool.model.TaskExecutionInfo;
import com.bytechef.automation.ai.tool.model.TriggerExecutionInfo;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionDetailInfo;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionSummary;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.exception.ProjectWorkflowExecutionErrorType;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
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
 * @author Ivica Cardic
 */
@Component
public class WorkflowExecutionTools {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionTools.class);

    private static final long NO_WORKSPACE_ID = 0L;

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;
    private final ScopedProjectGuard scopedProjectGuard;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowExecutionTools(
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade, ScopedProjectGuard scopedProjectGuard) {

        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
        this.scopedProjectGuard = scopedProjectGuard;
    }

    @Tool(
        description = "Get the full detail of a single workflow execution by id: job status, dates, job-level error, " +
            "the trigger execution, and every task execution with its input, output, and error. Use this to diagnose " +
            "why a specific run behaved the way it did. In an embedded chat confined to one project, only that " +
            "project's executions can be read.")
    public WorkflowExecutionDetailInfo getWorkflowExecution(
        @ToolParam(description = "The workflow execution id") long workflowExecutionId, ToolContext toolContext) {

        try {
            Long scopedProjectId = scopedProjectGuard.fetchScopedProjectId(toolContext);

            if (scopedProjectId != null) {
                WorkflowExecutionDTO execution = projectWorkflowExecutionFacade.getWorkflowExecution(
                    workflowExecutionId);

                Project project = execution == null ? null : execution.project();

                if (project == null || !scopedProjectId.equals(project.getId())) {
                    throw new ExecutionException(
                        "Workflow execution " + workflowExecutionId + " not found",
                        ProjectWorkflowExecutionErrorType.GET_WORKFLOW_EXECUTION);
                }

                return toDetailInfo(execution);
            }

            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            if (workspaceId == null) {
                log.warn("getWorkflowExecution(): workspace id absent from tool context; the execution was not read");

                throw new ExecutionException(
                    "Workspace context unavailable in this tool invocation, so workflow execution " +
                        workflowExecutionId + " could not be read. This is not a statement that the execution " +
                        "does not exist.",
                    ProjectWorkflowExecutionErrorType.GET_WORKFLOW_EXECUTION);
            }

            WorkflowExecutionDTO execution = projectWorkflowExecutionFacade.getWorkflowExecution(workflowExecutionId);

            Project project = execution == null ? null : execution.project();
            Long executionWorkspaceId = project == null ? null : project.getWorkspaceId();

            if (executionWorkspaceId == null || !workspaceId.equals(executionWorkspaceId)) {
                throw new ExecutionException(
                    "Workflow execution " + workflowExecutionId + " not found",
                    ProjectWorkflowExecutionErrorType.GET_WORKFLOW_EXECUTION);
            }

            return toDetailInfo(execution);
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception exception) {
            log.error("getWorkflowExecution(): Failed for id={}", workflowExecutionId, exception);

            throw new ExecutionException(
                "Failed to get workflow execution: " + exception.getMessage(), exception,
                ProjectWorkflowExecutionErrorType.GET_WORKFLOW_EXECUTION);
        }
    }

    @Tool(
        description = "List recent workflow executions for the current workspace. Use this to resolve a run the user " +
            "describes (\"the failed run yesterday\", \"the last execution of my onboarding workflow\") to a concrete "
            +
            "execution id. Returns a JSON array of {id, workflowLabel, projectName, status, startDate, endDate}, " +
            "newest first. Optional filters: jobStatus narrows to a single status (any value outside CREATED, " +
            "STARTED, COMPLETED, FAILED, STOPPED is an error, never a silently unfiltered listing); " +
            "workflowId filters to one workflow. In an embedded chat confined to one project, only that project's " +
            "executions are listed, and a workflowId of another project is an error.")
    public List<WorkflowExecutionSummary> listWorkflowExecutions(
        @ToolParam(
            required = false,
            description = "Optional job status filter; exactly one of CREATED, STARTED, COMPLETED, FAILED, STOPPED") @Nullable String jobStatus,
        @ToolParam(
            required = false,
            description = "Optional workflow id to filter a specific workflow's history") @Nullable String workflowId,
        ToolContext toolContext) {

        try {
            Long scopedProjectId = scopedProjectGuard.fetchScopedProjectId(toolContext);

            if (scopedProjectId != null) {
                if (workflowId != null) {
                    scopedProjectGuard.checkWorkflow(
                        workflowId, ScopedProjectGuard.WORKFLOW_VIEW, toolContext,
                        ProjectWorkflowExecutionErrorType.LIST_WORKFLOW_EXECUTIONS);
                }

                Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
                Status status = parseStatus(jobStatus);

                Page<WorkflowExecutionDTO> page = projectWorkflowExecutionFacade.getWorkflowExecutions(
                    null, environmentId, status, null, null, scopedProjectId, null, workflowId, null,
                    NO_WORKSPACE_ID, 0);

                return toSummaries(page);
            }

            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            if (workspaceId == null) {
                log.warn("listWorkflowExecutions(): workspace id absent from tool context; the listing was not run");

                throw new ExecutionException(
                    "Workspace context unavailable in this tool invocation, so no workflow executions were listed. " +
                        "This is not a statement that there are no executions.",
                    ProjectWorkflowExecutionErrorType.LIST_WORKFLOW_EXECUTIONS);
            }

            Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
            Status status = parseStatus(jobStatus);

            Page<WorkflowExecutionDTO> page = projectWorkflowExecutionFacade.getWorkflowExecutions(
                false, environmentId, status, null, null, null, null, workflowId, null, workspaceId, 0);

            return toSummaries(page);
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception exception) {
            log.error("listWorkflowExecutions(): Failed", exception);

            throw new ExecutionException(
                "Failed to list workflow executions: " + exception.getMessage(), exception,
                ProjectWorkflowExecutionErrorType.LIST_WORKFLOW_EXECUTIONS);
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

    private static List<WorkflowExecutionSummary> toSummaries(Page<WorkflowExecutionDTO> page) {
        return page.getContent()
            .stream()
            .map(WorkflowExecutionTools::toSummary)
            .toList();
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
            return Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ExecutionException(
                "Unknown jobStatus '" + value + "'. Valid values: " + Arrays.stream(Status.values())
                    .map(Status::name)
                    .collect(Collectors.joining(", ")),
                ProjectWorkflowExecutionErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }
}
