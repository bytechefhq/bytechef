/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Validate.notNull(workflowId, "'workflowId' must not be null");

        ProjectWorkflow project = new ProjectWorkflow(projectId, projectVersion, workflowId, workflowReferenceCode);

        return projectWorkflowRepository.save(project);
    }

    @Override
    public void deleteProjectWorkflows(List<Long> ids) {
        projectWorkflowRepository.deleteAllById(ids);
    }

    @Override
    public ProjectWorkflow getProjectWorkflow(long id) {
        return OptionalUtils.get(projectWorkflowRepository.findById(id));
    }

    @Override
    public String getProjectWorkflowId(long projectInstanceId, String workflowReferenceCode) {
        return OptionalUtils.get(
            projectWorkflowRepository
                .findByProjectInstanceIdAndWorkflowReferenceCode(projectInstanceId, workflowReferenceCode)
                .map(ProjectWorkflow::getWorkflowId));
    }

    @Override
    public List<Long> getProjectWorkflowIds(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getId)
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
    public List<String> getWorkflowIds(long projectId) {
        return projectWorkflowRepository.findAllByProjectId(projectId)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public List<String> getWorkflowIds(long projectId, int projectVersion) {
        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        return OptionalUtils.get(projectWorkflowRepository.findByWorkflowId(workflowId));
    }

    @Override
    public void removeWorkflow(long projectId, int projectVersion, String workflowId) {
        if (projectWorkflowRepository.countByProjectIdAndProjectVersion(projectId, projectVersion) == 1) {
            throw new ConfigurationException(
                "The last workflow id=%s cannot be deleted".formatted(workflowId),
                ProjectErrorType.REMOVE_LAST_WORKFLOW);
        }

        projectWorkflowRepository.findByProjectIdAndProjectVersionAndWorkflowId(projectId, projectVersion, workflowId)
            .ifPresent(projectWorkflow -> projectWorkflowRepository.deleteById(projectWorkflow.getId()));
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        Validate.notNull(projectWorkflow, "'projectWorkflow' must not be null");
        Validate.notNull(projectWorkflow.getId(), "'id' must not be null");

        ProjectWorkflow curProjectWorkflow = OptionalUtils.get(
            projectWorkflowRepository.findById(projectWorkflow.getId()));

        curProjectWorkflow.setProjectVersion(projectWorkflow.getProjectVersion());
        curProjectWorkflow.setWorkflowId(projectWorkflow.getWorkflowId());
        curProjectWorkflow.setWorkflowReferenceCode(projectWorkflow.getWorkflowReferenceCode());

        return projectWorkflowRepository.save(projectWorkflow);
    }
}
