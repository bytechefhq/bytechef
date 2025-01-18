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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectDeploymentWorkflowServiceImpl implements ProjectDeploymentWorkflowService {

    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowServiceImpl(
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository) {

        this.projectDeploymentWorkflowRepository = projectDeploymentWorkflowRepository;
    }

    @Override
    public ProjectDeploymentWorkflow create(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        return projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }

    @Override
    public List<ProjectDeploymentWorkflow> create(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        return projectDeploymentWorkflowRepository.saveAll(projectDeploymentWorkflows);
    }

    @Override
    public void delete(long id) {
        projectDeploymentWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectDeploymentWorkflow>
        fetchProjectDeploymentWorkflow(long projectDeploymentId, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        return projectDeploymentWorkflowRepository.findByProjectDeploymentIdAndWorkflowId(
            projectDeploymentId, workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectDeploymentWorkflowConnection> fetchProjectDeploymentWorkflowConnection(
        long projectDeploymentOd, String workflowId, String workflowNodeName, String workflowConnectionKey) {

        return getProjectDeploymentWorkflow(projectDeploymentOd, workflowId)
            .getConnections()
            .stream()
            .filter(projectDeploymentWorkflowConnection -> Objects
                .equals(projectDeploymentWorkflowConnection.getWorkflowNodeName(), workflowNodeName) &&
                Objects.equals(projectDeploymentWorkflowConnection.getKey(), workflowConnectionKey))
            .findFirst();
    }

    @Override
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentWorkflowId) {
        return OptionalUtils.get(projectDeploymentWorkflowRepository.findById(projectDeploymentWorkflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeploymentWorkflowConnection> getProjectDeploymentWorkflowConnections(
        long projectDeploymentId, String workflowId, String workflowNodeName) {

        return getProjectDeploymentWorkflow(projectDeploymentId, workflowId)
            .getConnections()
            .stream()
            .filter(
                projectDeploymentWorkflowConnection -> Objects.equals(
                    projectDeploymentWorkflowConnection.getWorkflowNodeName(), workflowNodeName))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentId, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        return OptionalUtils.get(
            projectDeploymentWorkflowRepository.findByProjectDeploymentIdAndWorkflowId(
                projectDeploymentId, workflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(long projectDeploymentId) {
        return projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(List<Long> projectDeploymentIds) {
        Validate.notNull(projectDeploymentIds, "'projectDeploymentIds' must not be null");

        return projectDeploymentWorkflowRepository.findAllByProjectDeploymentIdIn(projectDeploymentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectionUsed(long connectionId) {
        return !projectDeploymentWorkflowRepository
            .findProjectDeploymentWorkflowConnectionIdsByConnectionId(connectionId)
            .isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProjectDeploymentWorkflowEnabled(long projectDeploymentId, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            getProjectDeploymentWorkflow(projectDeploymentId, workflowId);

        return projectDeploymentWorkflow.isEnabled();
    }

    @Override
    public ProjectDeploymentWorkflow update(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        ProjectDeploymentWorkflow curProjectDeploymentWorkflow = OptionalUtils.get(
            projectDeploymentWorkflowRepository.findById(Validate.notNull(projectDeploymentWorkflow.getId(), "id")));

        curProjectDeploymentWorkflow.setConnections(projectDeploymentWorkflow.getConnections());
        curProjectDeploymentWorkflow.setEnabled(projectDeploymentWorkflow.isEnabled());
        curProjectDeploymentWorkflow.setInputs(projectDeploymentWorkflow.getInputs());
        curProjectDeploymentWorkflow.setWorkflowId(projectDeploymentWorkflow.getWorkflowId());
        curProjectDeploymentWorkflow.setVersion(projectDeploymentWorkflow.getVersion());

        return projectDeploymentWorkflowRepository.save(curProjectDeploymentWorkflow);
    }

    @Override
    public List<ProjectDeploymentWorkflow> update(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        Validate.notNull(projectDeploymentWorkflows, "'projectDeploymentWorkflows' must not be null");

        List<ProjectDeploymentWorkflow> updatedProjectDeploymentWorkflows = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            updatedProjectDeploymentWorkflows.add(update(projectDeploymentWorkflow));
        }

        return updatedProjectDeploymentWorkflows;
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository.findById(id)
            .orElseThrow();

        projectDeploymentWorkflow.setEnabled(enabled);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }
}
