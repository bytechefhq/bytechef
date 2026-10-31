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
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeFactory;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionDiff;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import java.util.ArrayList;
import java.util.Comparator;
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
class ProjectVersionWorkflowChangeServiceImpl implements ProjectVersionWorkflowChangeService {

    private static final Comparator<VersionWorkflowChangeSummary> SUMMARY_COMPARATOR = Comparator
        .comparingInt((VersionWorkflowChangeSummary summary) -> changeTypeOrder(summary.changeType()))
        .thenComparing(
            VersionWorkflowChangeSummary::label, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

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
        Project project = getProject(projectId, projectVersion);

        Integer baseVersion = getBaseVersion(project, projectVersion);

        Map<String, Workflow> oldWorkflows = getWorkflows(projectId, baseVersion);
        Map<String, Workflow> newWorkflows = getWorkflows(projectId, projectVersion);

        List<VersionWorkflowChangeSummary> summaries = new ArrayList<>();

        for (String workflowUuid : getWorkflowUuids(oldWorkflows, newWorkflows)) {
            Workflow oldWorkflow = oldWorkflows.get(workflowUuid);
            Workflow newWorkflow = newWorkflows.get(workflowUuid);

            WorkflowDefinitionDiff diff = workflowDefinitionDiffService.diff(oldWorkflow, newWorkflow);

            if (diff.changeType() != WorkflowChangeType.UNCHANGED) {
                summaries.add(VersionWorkflowChangeFactory.summary(workflowUuid, oldWorkflow, newWorkflow, diff));
            }
        }

        summaries.sort(SUMMARY_COMPARATOR);

        return new VersionWorkflowChanges(baseVersion, projectVersion, summaries);
    }

    @Override
    public VersionWorkflowChange getVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid) {
        Project project = getProject(projectId, projectVersion);

        Integer baseVersion = getBaseVersion(project, projectVersion);

        Workflow oldWorkflow = getWorkflows(projectId, baseVersion).get(workflowUuid);
        Workflow newWorkflow = getWorkflows(projectId, projectVersion).get(workflowUuid);

        if (oldWorkflow == null && newWorkflow == null) {
            throw new IllegalArgumentException(
                "Workflow uuid=%s not found in project version v=%s".formatted(workflowUuid, projectVersion));
        }

        WorkflowDefinitionDiff diff = workflowDefinitionDiffService.diff(oldWorkflow, newWorkflow);

        return VersionWorkflowChangeFactory.change(
            workflowUuid, baseVersion, projectVersion, oldWorkflow, newWorkflow, diff);
    }

    private static int changeTypeOrder(WorkflowChangeType changeType) {
        return switch (changeType) {
            case ADDED -> 0;
            case MODIFIED -> 1;
            case REMOVED -> 2;
            case UNCHANGED -> 3;
        };
    }

    private @Nullable Integer getBaseVersion(Project project, int projectVersion) {
        Integer baseVersion = null;

        for (ProjectVersion existingProjectVersion : project.getProjectVersions()) {
            int version = existingProjectVersion.getVersion();

            if (version < projectVersion && (baseVersion == null || version > baseVersion)) {
                baseVersion = version;
            }
        }

        return baseVersion;
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

    private static List<String>
        getWorkflowUuids(Map<String, Workflow> oldWorkflows, Map<String, Workflow> newWorkflows) {
        Map<String, Boolean> workflowUuids = new LinkedHashMap<>();

        for (String workflowUuid : oldWorkflows.keySet()) {
            workflowUuids.put(workflowUuid, Boolean.TRUE);
        }

        for (String workflowUuid : newWorkflows.keySet()) {
            workflowUuids.put(workflowUuid, Boolean.TRUE);
        }

        return new ArrayList<>(workflowUuids.keySet());
    }
}
