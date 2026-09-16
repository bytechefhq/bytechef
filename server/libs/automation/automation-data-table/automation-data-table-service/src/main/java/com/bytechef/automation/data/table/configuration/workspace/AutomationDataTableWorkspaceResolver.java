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

package com.bytechef.automation.data.table.configuration.workspace;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AutomationDataTableWorkspaceResolver implements DataTableWorkspaceResolver {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public AutomationDataTableWorkspaceResolver(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public OptionalLong resolveByWorkflowId(String workflowId) {
        return projectService.fetchWorkflowProject(workflowId)
            .map(Project::getWorkspaceId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }

    @Override
    public OptionalLong resolveByJobPrincipalId(long jobPrincipalId, PlatformType platformType) {
        if (platformType != PlatformType.AUTOMATION) {
            return OptionalLong.empty();
        }

        Project project = projectService.getProjectDeploymentProject(jobPrincipalId);

        return OptionalLong.of(project.getWorkspaceId());
    }

    @Override
    public OptionalLong resolveByWorkflowUuid(String workflowUuid) {
        String workflowId;

        try {
            workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        } catch (IllegalArgumentException illegalArgumentException) {
            return OptionalLong.empty();
        }

        return resolveByWorkflowId(workflowId);
    }
}
