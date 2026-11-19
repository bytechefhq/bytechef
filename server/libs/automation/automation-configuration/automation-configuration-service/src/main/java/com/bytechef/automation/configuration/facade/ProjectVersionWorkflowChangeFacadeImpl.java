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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeCalculator;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
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
public class ProjectVersionWorkflowChangeFacadeImpl implements ProjectVersionWorkflowChangeFacade {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowDefinitionDiffService workflowDefinitionDiffService;
    private final WorkflowService workflowService;

    ProjectVersionWorkflowChangeFacadeImpl(
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

        List<ProjectWorkflow> baseProjectWorkflows = getProjectWorkflows(projectId, baseVersion);
        List<ProjectWorkflow> projectWorkflows = getProjectWorkflows(projectId, projectVersion);

        Map<String, Workflow> workflowsById = getWorkflowsById(baseProjectWorkflows, projectWorkflows);

        return VersionWorkflowChangeCalculator.summarize(
            baseVersion, projectVersion, getWorkflows(baseProjectWorkflows, workflowsById),
            getWorkflows(projectWorkflows, workflowsById), workflowDefinitionDiffService);
    }

    @Override
    public VersionWorkflowChange getVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid) {
        Integer baseVersion = getBaseVersion(projectId, projectVersion);

        List<ProjectWorkflow> baseProjectWorkflows = getProjectWorkflows(projectId, baseVersion);
        List<ProjectWorkflow> projectWorkflows = getProjectWorkflows(projectId, projectVersion);

        Map<String, Workflow> workflowsById = getWorkflowsById(baseProjectWorkflows, projectWorkflows);

        return VersionWorkflowChangeCalculator.fetchChange(
            baseVersion, projectVersion, workflowUuid, getWorkflows(baseProjectWorkflows, workflowsById),
            getWorkflows(projectWorkflows, workflowsById), workflowDefinitionDiffService)
            .orElseThrow(() -> new IllegalArgumentException(
                "Workflow uuid=%s not found in project version v=%s".formatted(workflowUuid, projectVersion)));
    }

    @Override
    public boolean hasUnpublishedChanges(Project project) {
        int draftProjectVersion = project.getLastProjectVersion();

        Integer baseVersion = VersionWorkflowChangeCalculator.resolveBaseVersion(
            getVersions(project), draftProjectVersion);

        if (baseVersion == null) {
            return true;
        }

        long projectId = project.getId();

        List<ProjectWorkflow> baseProjectWorkflows = getProjectWorkflows(projectId, baseVersion);
        List<ProjectWorkflow> draftProjectWorkflows = getProjectWorkflows(projectId, draftProjectVersion);

        Map<String, Workflow> workflowsById = getWorkflowsById(baseProjectWorkflows, draftProjectWorkflows);

        return VersionWorkflowChangeCalculator.hasChanges(
            fetchWorkflows(baseProjectWorkflows, workflowsById), fetchWorkflows(draftProjectWorkflows, workflowsById),
            workflowDefinitionDiffService);
    }

    private static List<Integer> getVersions(Project project) {
        List<Integer> versions = new ArrayList<>();

        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            versions.add(projectVersion.getVersion());
        }

        return versions;
    }

    private @Nullable Integer getBaseVersion(long projectId, int projectVersion) {
        Project project = getProject(projectId, projectVersion);

        return VersionWorkflowChangeCalculator.resolveBaseVersion(getVersions(project), projectVersion);
    }

    private Project getProject(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        project.fetchProjectVersion(projectVersion)
            .orElseThrow(() -> new ConfigurationException(
                "Project version v=%s not found".formatted(projectVersion),
                ProjectErrorType.PROJECT_VERSION_NOT_FOUND));

        return project;
    }

    private static Map<String, @Nullable Workflow> fetchWorkflows(
        List<ProjectWorkflow> projectWorkflows, Map<String, Workflow> workflowsById) {

        Map<String, @Nullable Workflow> workflows = new LinkedHashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            workflows.put(projectWorkflow.getUuidAsString(), workflowsById.get(projectWorkflow.getWorkflowId()));
        }

        return workflows;
    }

    private List<ProjectWorkflow> getProjectWorkflows(long projectId, @Nullable Integer projectVersion) {
        if (projectVersion == null) {
            return List.of();
        }

        return projectWorkflowService.getProjectWorkflows(projectId, projectVersion);
    }

    private Map<String, Workflow> getWorkflows(
        List<ProjectWorkflow> projectWorkflows, Map<String, Workflow> workflowsById) {

        Map<String, Workflow> workflows = new LinkedHashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            String workflowId = projectWorkflow.getWorkflowId();

            Workflow workflow = workflowsById.get(workflowId);

            if (workflow == null) {
                workflow = workflowService.getWorkflow(workflowId);
            }

            workflows.put(projectWorkflow.getUuidAsString(), workflow);
        }

        return workflows;
    }

    private Map<String, Workflow> getWorkflowsById(
        List<ProjectWorkflow> baseProjectWorkflows, List<ProjectWorkflow> projectWorkflows) {

        Set<String> workflowIds = new LinkedHashSet<>();

        for (ProjectWorkflow projectWorkflow : baseProjectWorkflows) {
            workflowIds.add(projectWorkflow.getWorkflowId());
        }

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            workflowIds.add(projectWorkflow.getWorkflowId());
        }

        Map<String, Workflow> workflowsById = new HashMap<>();

        for (Workflow workflow : workflowService.getWorkflows(List.copyOf(workflowIds))) {
            workflowsById.put(Validate.notNull(workflow.getId(), "id"), workflow);
        }

        return workflowsById;
    }
}
