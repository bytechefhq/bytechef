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

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectWorkflowServiceImpl implements ProjectWorkflowService {

    private final ProjectWorkflowRepository projectWorkflowRepository;

    public ProjectWorkflowServiceImpl(ProjectWorkflowRepository projectWorkflowRepository) {
        this.projectWorkflowRepository = projectWorkflowRepository;
    }

    @Override
    public ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        return projectWorkflowRepository.save(new ProjectWorkflow(projectId, projectVersion, workflowId));
    }

    @Override
    public void delete(List<Long> ids) {
        projectWorkflowRepository.deleteAllById(ids);
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
    public void delete(long projectId, int projectVersion, String workflowId) {
        projectWorkflowRepository.findByProjectIdAndProjectVersionAndWorkflowId(projectId, projectVersion, workflowId)
            .ifPresent(projectWorkflow -> projectWorkflowRepository.deleteById(projectWorkflow.getId()));
    }

    @Override
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

        return projectWorkflowRepository.save(curProjectWorkflow);
    }
}
