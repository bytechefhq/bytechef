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

import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditPublisher;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectWorkflowServiceImpl implements ProjectWorkflowService {

    private final ProjectWorkflowAuditPublisher projectWorkflowAuditPublisher;
    private final ProjectWorkflowRepository projectWorkflowRepository;

    public ProjectWorkflowServiceImpl(
        ProjectWorkflowAuditPublisher projectWorkflowAuditPublisher,
        ProjectWorkflowRepository projectWorkflowRepository) {

        this.projectWorkflowAuditPublisher = projectWorkflowAuditPublisher;
        this.projectWorkflowRepository = projectWorkflowRepository;
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE')")
    public ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        ProjectWorkflow savedProjectWorkflow = projectWorkflowRepository.save(
            new ProjectWorkflow(projectId, projectVersion, workflowId));

        Map<String, Object> data = new HashMap<>();

        data.put("projectId", String.valueOf(savedProjectWorkflow.getProjectId()));
        data.put("workflowId", savedProjectWorkflow.getWorkflowId());

        projectWorkflowAuditPublisher.publish(
            ProjectWorkflowAuditEvent.WORKFLOW_CREATED, savedProjectWorkflow.getId(), data);

        return savedProjectWorkflow;
    }

    @Override
    public void delete(List<Long> ids) {
        projectWorkflowRepository.deleteAllById(ids);

        for (Long id : ids) {
            projectWorkflowAuditPublisher.publish(ProjectWorkflowAuditEvent.WORKFLOW_DELETED, id);
        }
    }

    @Override
    public Optional<String> fetchLastProjectWorkflowId(Long projectId, String workflowUuid) {
        return projectWorkflowRepository.findLastByProjectIdAndUuid(projectId, UUID.fromString(workflowUuid))
            .map(ProjectWorkflow::getWorkflowId);
    }

    @Override
    public Optional<ProjectWorkflow> fetchProjectWorkflow(long projectId, int projectVersion, String workflowUuid) {
        return projectWorkflowRepository.findByProjectIdAndProjectVersionAndUuid(
            projectId, projectVersion, UUID.fromString(workflowUuid));
    }

    @Override
    public ProjectWorkflow getLastProjectWorkflow(long projectId, String workflowUuid) {
        return projectWorkflowRepository.findLastByProjectIdAndUuid(projectId, UUID.fromString(workflowUuid))
            .orElseThrow(() -> new IllegalArgumentException("No workflow found for project id " + projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectWorkflow> fetchWorkflowProjectWorkflow(String workflowId) {
        return projectWorkflowRepository.findByWorkflowId(workflowId);
    }

    @Override
    public String getLastPublishedWorkflowId(String workflowUuid) {
        return projectWorkflowRepository
            .findLastPublishedByUuid(UUID.fromString(workflowUuid))
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException("No published workflow found for workflow uuid " + workflowUuid));
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        return projectWorkflowRepository
            .findLastByUuid(UUID.fromString(workflowUuid))
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalArgumentException("No workflow found for workflow uuid " + workflowUuid));
    }

    @Override
    public List<ProjectWorkflow> getLatestProjectWorkflows() {
        return projectWorkflowRepository.findAllLatestPerUuid();
    }

    @Override
    public List<Long> getProjectProjectWorkflowIds(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getId)
            .toList();
    }

    @Override
    public ProjectWorkflow getProjectWorkflow(long id) {
        return projectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId) {
        return projectWorkflowRepository.findAllByProjectId(projectId)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows() {
        return projectWorkflowRepository.findAll();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(List<Long> projectIds) {
        return projectWorkflowRepository.findAllByProjectIdIn(projectIds);
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId) {
        return projectWorkflowRepository.findAllByProjectId(projectId);
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion);
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(Long projectId, String workflowUuid) {
        return projectWorkflowRepository.findAllByProjectIdAndUuid(
            projectId, UUID.fromString(workflowUuid));
    }

    @Override
    public String getProjectWorkflowWorkflowId(long projectDeploymentId, String workflowUuid) {
        return projectWorkflowRepository
            .findByProjectDeploymentIdAndUuid(projectDeploymentId, UUID.fromString(workflowUuid))
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));
    }

    @Override
    public Optional<String> fetchProjectWorkflowWorkflowId(long projectDeploymentId, String workflowUuid) {
        return projectWorkflowRepository
            .findByProjectDeploymentIdAndUuid(projectDeploymentId, UUID.fromString(workflowUuid))
            .map(ProjectWorkflow::getWorkflowId);
    }

    @Override
    public String getProjectWorkflowUuid(long projectDeploymentId, String workflowId) {
        return projectWorkflowRepository
            .findByProjectDeploymentIdAndWorkflowId(projectDeploymentId, workflowId)
            .map(ProjectWorkflow::getUuidAsString)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        return projectWorkflowRepository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));
    }

    @Override
    public List<ProjectWorkflow> getWorkflowProjectWorkflows(List<String> workflowIds) {
        if (workflowIds.isEmpty()) {
            return List.of();
        }

        return projectWorkflowRepository.findAllByWorkflowIdIn(workflowIds);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_DELETE')")
    public void delete(long projectId, int projectVersion, String workflowId) {
        projectWorkflowRepository.findByProjectIdAndProjectVersionAndWorkflowId(projectId, projectVersion, workflowId)
            .ifPresent(projectWorkflow -> {
                long projectWorkflowId = projectWorkflow.getId();

                projectWorkflowRepository.deleteById(projectWorkflowId);

                Map<String, Object> data = new HashMap<>();

                data.put("projectId", String.valueOf(projectWorkflow.getProjectId()));
                data.put("workflowId", projectWorkflow.getWorkflowId());

                projectWorkflowAuditPublisher.publish(
                    ProjectWorkflowAuditEvent.WORKFLOW_DELETED, projectWorkflowId, data);
            });
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'DEPLOYMENT_PUSH')")
    public void publishWorkflow(
        long projectId, int oldProjectVersion, String oldWorkflowId, ProjectWorkflow projectWorkflow) {

        Assert.notNull(projectWorkflow, "'projectWorkflow' must not be null");

        update(projectWorkflow);

        projectWorkflow = new ProjectWorkflow(
            projectId, oldProjectVersion, oldWorkflowId, UUID.fromString(projectWorkflow.getUuidAsString()));

        projectWorkflowRepository.save(projectWorkflow);
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        Assert.notNull(projectWorkflow, "'projectWorkflow' must not be null");
        Assert.notNull(projectWorkflow.getId(), "'id' must not be null");

        ProjectWorkflow curProjectWorkflow = projectWorkflowRepository.findById(projectWorkflow.getId())
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));

        curProjectWorkflow.setProjectVersion(projectWorkflow.getProjectVersion());
        curProjectWorkflow.setWorkflowId(projectWorkflow.getWorkflowId());
        curProjectWorkflow.setUuid(projectWorkflow.getUuidAsString());

        ProjectWorkflow savedProjectWorkflow = projectWorkflowRepository.save(curProjectWorkflow);

        Map<String, Object> data = new HashMap<>();

        data.put("workflowId", savedProjectWorkflow.getWorkflowId());

        projectWorkflowAuditPublisher.publish(
            ProjectWorkflowAuditEvent.WORKFLOW_UPDATED, savedProjectWorkflow.getId(), data);

        return savedProjectWorkflow;
    }

    @Override
    public ProjectWorkflow updateErrorWorkflow(
        long id, @Nullable Long errorProjectWorkflowId, boolean errorWorkflowDisabled) {

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));

        projectWorkflow.setErrorProjectWorkflowId(errorProjectWorkflowId);
        projectWorkflow.setErrorWorkflowDisabled(errorWorkflowDisabled);

        return projectWorkflowRepository.save(projectWorkflow);
    }

    @Override
    public ProjectWorkflow updatePermissionExpression(long id, @Nullable String permissionExpression) {
        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));

        projectWorkflow.setPermissionExpression(permissionExpression);

        return projectWorkflowRepository.save(projectWorkflow);
    }
}
