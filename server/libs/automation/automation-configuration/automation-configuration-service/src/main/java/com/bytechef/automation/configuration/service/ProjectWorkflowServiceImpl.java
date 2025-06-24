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
import com.bytechef.commons.util.OptionalUtils;
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
        return addWorkflow(projectId, projectVersion, workflowId, String.valueOf(UUID.randomUUID()));
    }

    @Override
    public ProjectWorkflow addWorkflow(
        long projectId, int projectVersion, String workflowId, String workflowReferenceCode) {

        Assert.notNull(workflowId, "'workflowId' must not be null");

        ProjectWorkflow project = new ProjectWorkflow(projectId, projectVersion, workflowId, workflowReferenceCode);

        return projectWorkflowRepository.save(project);
    }

    @Override
    public void delete(List<Long> ids) {
        projectWorkflowRepository.deleteAllById(ids);
    }

    @Override
    public Optional<String> fetchLatestProjectWorkflowId(Long projectId, String workflowReferenceCode) {
        return projectWorkflowRepository.findByProjectIdAndWorkflowReferenceCode(projectId, workflowReferenceCode)
            .map(ProjectWorkflow::getWorkflowId);
    }

    @Override
    public Optional<ProjectWorkflow>
        fetchProjectWorkflow(long projectId, int projectVersion, String workflowReferenceCode) {
        return projectWorkflowRepository.findByProjectIdAndProjectVersionAndWorkflowReferenceCode(
            projectId, projectVersion, workflowReferenceCode);
    }

    @Override
    public ProjectWorkflow getLatestProjectWorkflow(Long projectId, String workflowReferenceCode) {
        return OptionalUtils.get(
            projectWorkflowRepository.findByProjectIdAndWorkflowReferenceCode(projectId, workflowReferenceCode));
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode) {
        return OptionalUtils.get(
            projectWorkflowRepository
                .findLatestProjectWorkflowByWorkflowReferenceCode(workflowReferenceCode)
                .map(ProjectWorkflow::getWorkflowId));
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
        return OptionalUtils.get(projectWorkflowRepository.findById(id));
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
    public List<ProjectWorkflow> getProjectWorkflows(long projectId) {
        return projectWorkflowRepository.findAllByProjectId(projectId);
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion);
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(Long projectId, String workflowReferenceCode) {
        return projectWorkflowRepository.findAllByProjectIdAndWorkflowReferenceCode(
            projectId, workflowReferenceCode);
    }

    @Override
    public String getProjectDeploymentWorkflowId(long projectDeploymentId, String workflowReferenceCode) {
        return OptionalUtils.get(
            projectWorkflowRepository
                .findByProjectDeploymentIdAndWorkflowReferenceCode(projectDeploymentId, workflowReferenceCode)
                .map(ProjectWorkflow::getWorkflowId));
    }

    @Override
    public String getProjectDeploymentWorkflowReferenceCode(long projectDeploymentId, String workflowId) {
        return OptionalUtils.get(
            projectWorkflowRepository
                .findByProjectDeploymentIdAndWorkflowId(projectDeploymentId, workflowId)
                .map(ProjectWorkflow::getWorkflowReferenceCode));
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        return OptionalUtils.get(projectWorkflowRepository.findByWorkflowId(workflowId));
    }

    @Override
    public void delete(long projectId, int projectVersion, String workflowId) {
        projectWorkflowRepository.findByProjectIdAndProjectVersionAndWorkflowId(projectId, projectVersion, workflowId)
            .ifPresent(projectWorkflow -> projectWorkflowRepository.deleteById(projectWorkflow.getId()));
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        Assert.notNull(projectWorkflow, "'projectWorkflow' must not be null");
        Assert.notNull(projectWorkflow.getId(), "'id' must not be null");

        ProjectWorkflow curProjectWorkflow = OptionalUtils.get(
            projectWorkflowRepository.findById(projectWorkflow.getId()));

        curProjectWorkflow.setProjectVersion(projectWorkflow.getProjectVersion());
        curProjectWorkflow.setWorkflowId(projectWorkflow.getWorkflowId());
        curProjectWorkflow.setWorkflowReferenceCode(projectWorkflow.getWorkflowReferenceCode());

        return projectWorkflowRepository.save(projectWorkflow);
    }
}
