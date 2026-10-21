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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code buildWorkflow} runs with a {@code kind} on DRAFT workflows. The management MCP wrapper sets
 * {@link #APP_CALLABLE_KEY} in the context it forwards; the workflow-writing tools the specialist calls then refuse a
 * workflow of a published version and name the DRAFT workflow with the same uuid, so an agent holding an id from before
 * {@code publishProject} cannot change what is deployed through such a {@code buildWorkflow} run. Only that mode is
 * guarded: other surfaces, the flat management MCP workflow tools included, never set the flag. Callers run the guard
 * after their own authorized read of the workflow, so a caller without access gets that read's denial and never the
 * project, version or DRAFT id the refusal names.
 *
 * @author Ivica Cardic
 */
@Component
public class AppCallableDraftGuard {

    public static final String APP_CALLABLE_KEY = "bytechef.workflowEditor.appCallable";

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public AppCallableDraftGuard(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    public void checkDraft(@Nullable ToolContext toolContext, String workflowId) {
        if (!isAppCallable(toolContext)) {
            return;
        }

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
        Project project = projectService.getProject(projectWorkflow.getProjectId());

        if (projectWorkflow.getProjectVersion() == project.getLastProjectVersion()) {
            return;
        }

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(projectWorkflow.getUuidAsString());

        throw new IllegalStateException(
            ("Workflow %s belongs to published version %d of project %d and cannot be edited; edit the DRAFT " +
                "workflow %s instead").formatted(
                    workflowId, projectWorkflow.getProjectVersion(), projectWorkflow.getProjectId(),
                    draftWorkflowId));
    }

    static boolean isAppCallable(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return false;
        }

        Map<String, Object> context = toolContext.getContext();

        return context != null && Boolean.TRUE.equals(context.get(APP_CALLABLE_KEY));
    }
}
