/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.tool.PersistedWorkflowChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CheckWorkflowTools {

    private final PersistedWorkflowChecker persistedWorkflowChecker;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public CheckWorkflowTools(PersistedWorkflowChecker persistedWorkflowChecker, ProjectService projectService) {
        this.persistedWorkflowChecker = persistedWorkflowChecker;
        this.projectService = projectService;
    }

    @Tool(
        description = "Check a saved workflow the way the workflow editor does and return every error it would flag: " +
            "missing connections, data pills that reference nothing, unknown resources such as data tables, invalid " +
            "parameters. An empty issues list means the editor shows no errors. Issues with userAction true need " +
            "the user, such as a connection to create. Warnings are the editor's warnings: name each one to the user, "
            +
            "and for a reference into a task whose output is only known after it runs, such as a script task, say a " +
            "test run will confirm it. Call it before describing an existing workflow as working, complete or ready.")
    public WorkflowCheck checkWorkflow(
        @ToolParam(description = "The id of the workflow to check") String workflowId, ToolContext toolContext) {

        AutomationToolInvocationContext context = AutomationToolInvocationContext.fromToolContext(toolContext);
        Long workspaceId = context == null ? null : context.workspaceId();

        if (workspaceId == null) {
            throw new IllegalStateException(
                "The workspace of this conversation could not be determined, so the workflow was not checked.");
        }

        Project project = fetchWorkflowProject(workflowId);

        if (project == null || !Objects.equals(project.getWorkspaceId(), workspaceId)) {
            throw new IllegalArgumentException("Workflow " + workflowId + " was not found. Check the workflow id.");
        }

        return new WorkflowCheck(
            workflowId, persistedWorkflowChecker.check(workflowId), persistedWorkflowChecker.checkWarnings(workflowId));
    }

    private @Nullable Project fetchWorkflowProject(String workflowId) {
        try {
            return projectService.getWorkflowProject(workflowId);
        } catch (NoSuchElementException exception) {
            return null;
        }
    }

    public record WorkflowCheck(
        String workflowId, List<PersistedWorkflowChecker.Issue> issues, List<PersistedWorkflowChecker.Issue> warnings) {

        public WorkflowCheck {
            issues = List.copyOf(issues);
            warnings = List.copyOf(warnings);
        }
    }
}
