
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.repository.ProjectInstanceWorkflowConnectionRepository;
import com.bytechef.helios.configuration.repository.ProjectInstanceWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectInstanceWorkflowServiceImpl implements ProjectInstanceWorkflowService {

    private final ProjectInstanceWorkflowConnectionRepository projectInstanceWorkflowConnectionRepository;
    private final ProjectInstanceWorkflowRepository projectInstanceWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectInstanceWorkflowServiceImpl(
        ProjectInstanceWorkflowConnectionRepository projectInstanceWorkflowConnectionRepository,
        ProjectInstanceWorkflowRepository projectInstanceWorkflowRepository) {

        this.projectInstanceWorkflowConnectionRepository = projectInstanceWorkflowConnectionRepository;
        this.projectInstanceWorkflowRepository = projectInstanceWorkflowRepository;
    }

    @Override
    public List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        return projectInstanceWorkflowRepository.saveAll(projectInstanceWorkflows);
    }

    @Override
    public void delete(Long id) {
        projectInstanceWorkflowRepository.deleteById(id);
    }

    @Override
    public boolean isProjectInstanceWorkflowEnabled(long projectInstanceId, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = getProjectInstanceWorkflow(projectInstanceId, workflowId);

        return projectInstanceWorkflow.isEnabled();
    }

    @Override
    public ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(
        String workflowConnectionOperationName, String workflowConnectionKey) {

        return OptionalUtils.get(projectInstanceWorkflowConnectionRepository.findByKeyAndOperationName(
            workflowConnectionKey, workflowConnectionOperationName));
    }

    @Override
    public long getProjectInstanceWorkflowConnectionId(
        String workflowConnectionOperationName, String workflowConnectionKey) {

        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection = getProjectInstanceWorkflowConnection(
            workflowConnectionOperationName, workflowConnectionKey);

        return projectInstanceWorkflowConnection.getConnectionId();
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        return OptionalUtils.get(
            projectInstanceWorkflowRepository.findByProjectInstanceIdAndWorkflowId(projectInstanceId, workflowId));
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId) {
        return projectInstanceWorkflowRepository.findAllByProjectInstanceId(projectInstanceId);
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds) {
        Validate.notNull(projectInstanceIds, "'projectInstanceIds' must not be null");

        return projectInstanceWorkflowRepository.findAllByProjectInstanceIdIn(projectInstanceIds);
    }

    @Override
    public ProjectInstanceWorkflow update(ProjectInstanceWorkflow projectInstanceWorkflow) {
        ProjectInstanceWorkflow curProjectInstanceWorkflow = OptionalUtils.get(
            projectInstanceWorkflowRepository.findById(Validate.notNull(projectInstanceWorkflow.getId(), "id")));

        curProjectInstanceWorkflow.setConnections(projectInstanceWorkflow.getConnections());
        curProjectInstanceWorkflow.setEnabled(projectInstanceWorkflow.isEnabled());
        curProjectInstanceWorkflow.setInputs(projectInstanceWorkflow.getInputs());
        curProjectInstanceWorkflow.setVersion(projectInstanceWorkflow.getVersion());

        return projectInstanceWorkflowRepository.save(curProjectInstanceWorkflow);
    }

    @Override
    public List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        Validate.notNull(projectInstanceWorkflows, "'projectInstanceWorkflows' must not be null");

        List<ProjectInstanceWorkflow> updatedProjectInstanceWorkflows = new ArrayList<>();

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            updatedProjectInstanceWorkflows.add(update(projectInstanceWorkflow));
        }

        return updatedProjectInstanceWorkflows;
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowRepository.findById(id)
            .orElseThrow();

        projectInstanceWorkflow.setEnabled(enabled);

        projectInstanceWorkflowRepository.save(projectInstanceWorkflow);
    }
}
