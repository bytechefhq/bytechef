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

import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Confines a run that carries a scoped project to that project's workflows.
 *
 * @author Ivica Cardic
 */
@Component
public class ScopedProjectGuard {

    static final String OUT_OF_SCOPE_MESSAGE = "This chat can only work with the workflows of its own project.";

    private final @Nullable ProjectWorkflowService projectWorkflowService;

    @Autowired
    @SuppressFBWarnings("EI")
    public ScopedProjectGuard(ObjectProvider<ProjectWorkflowService> projectWorkflowServiceProvider) {
        this(projectWorkflowServiceProvider.getIfAvailable());
    }

    @SuppressFBWarnings("EI")
    public ScopedProjectGuard(@Nullable ProjectWorkflowService projectWorkflowService) {
        this.projectWorkflowService = projectWorkflowService;
    }

    public void checkWorkflow(String workflowId, @Nullable ToolContext toolContext, ErrorType errorType) {
        Long scopedProjectId = ScopedProjectToolContext.fetchScopedProjectId(toolContext);

        if (scopedProjectId == null) {
            return;
        }

        Long workflowProjectId = null;

        if (projectWorkflowService != null) {
            try {
                ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

                workflowProjectId = projectWorkflow.getProjectId();
            } catch (RuntimeException exception) {
                workflowProjectId = null;
            }
        }

        if (!Objects.equals(scopedProjectId, workflowProjectId)) {
            throw new ExecutionException(OUT_OF_SCOPE_MESSAGE, errorType);
        }
    }

    public void checkProject(long projectId, @Nullable ToolContext toolContext, ErrorType errorType) {
        Long scopedProjectId = ScopedProjectToolContext.fetchScopedProjectId(toolContext);

        if (scopedProjectId != null && scopedProjectId != projectId) {
            throw new ExecutionException(OUT_OF_SCOPE_MESSAGE, errorType);
        }
    }

    public @Nullable Long fetchScopedProjectId(@Nullable ToolContext toolContext) {
        return ScopedProjectToolContext.fetchScopedProjectId(toolContext);
    }
}
