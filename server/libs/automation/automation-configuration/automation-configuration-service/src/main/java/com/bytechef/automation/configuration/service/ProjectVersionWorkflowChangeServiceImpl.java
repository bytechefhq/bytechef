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

package com.bytechef.automation.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeCalculator;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unguarded: callers must be guarded facades.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class ProjectVersionWorkflowChangeServiceImpl implements ProjectVersionWorkflowChangeService {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowDefinitionDiffService workflowDefinitionDiffService;
    private final WorkflowService workflowService;

    ProjectVersionWorkflowChangeServiceImpl(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowDefinitionDiffService workflowDefinitionDiffService, WorkflowService workflowService) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowDefinitionDiffService = workflowDefinitionDiffService;
        this.workflowService = workflowService;
    }

    @Override
    public VersionWorkflowChanges getVersionWorkflowChanges(long projectId, int projectVersion) {
        Integer baseVersion = getBaseVersion(projectId, projectVersion);

        return VersionWorkflowChangeCalculator.summarize(
            baseVersion, projectVersion, getWorkflows(projectId, baseVersion), getWorkflows(projectId, projectVersion),
            workflowDefinitionDiffService);
    }

    @Override
    public VersionWorkflowChange getVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid) {
        Integer baseVersion = getBaseVersion(projectId, projectVersion);

        return VersionWorkflowChangeCalculator.fetchChange(
            baseVersion, projectVersion, workflowUuid, getWorkflows(projectId, baseVersion),
            getWorkflows(projectId, projectVersion), workflowDefinitionDiffService)
            .orElseThrow(() -> new IllegalArgumentException(
                "Workflow uuid=%s not found in project version v=%s".formatted(workflowUuid, projectVersion)));
    }

    private @Nullable Integer getBaseVersion(long projectId, int projectVersion) {
        Project project = getProject(projectId, projectVersion);

        List<Integer> versions = new ArrayList<>();

        for (ProjectVersion existingProjectVersion : project.getProjectVersions()) {
            versions.add(existingProjectVersion.getVersion());
        }

        return VersionWorkflowChangeCalculator.resolveBaseVersion(versions, projectVersion);
    }

    private Project getProject(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        project.fetchProjectVersion(projectVersion)
            .orElseThrow(() -> new ConfigurationException(
                "Project version v=%s not found".formatted(projectVersion),
                ProjectErrorType.PROJECT_VERSION_NOT_FOUND));

        return project;
    }

    private Map<String, Workflow> getWorkflows(long projectId, @Nullable Integer projectVersion) {
        Map<String, Workflow> workflows = new LinkedHashMap<>();

        if (projectVersion == null) {
            return workflows;
        }

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(
            projectId, projectVersion)) {

            workflows.put(
                projectWorkflow.getUuidAsString(), workflowService.getWorkflow(projectWorkflow.getWorkflowId()));
        }

        return workflows;
    }
}
