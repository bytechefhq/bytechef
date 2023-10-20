
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

package com.bytechef.helios.project.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowJob;
import com.bytechef.helios.project.repository.ProjectInstanceWorkflowConnectionRepository;
import com.bytechef.helios.project.repository.ProjectInstanceWorkflowJobRepository;
import com.bytechef.helios.project.repository.ProjectInstanceWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectInstanceWorkflowServiceImpl implements ProjectInstanceWorkflowService {

    private final ProjectInstanceWorkflowConnectionRepository projectInstanceWorkflowConnectionRepository;
    private final ProjectInstanceWorkflowJobRepository projectInstanceWorkflowJobRepository;
    private final ProjectInstanceWorkflowRepository projectInstanceWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectInstanceWorkflowServiceImpl(
        ProjectInstanceWorkflowConnectionRepository projectInstanceWorkflowConnectionRepository,
        ProjectInstanceWorkflowJobRepository projectInstanceWorkflowJobRepository,
        ProjectInstanceWorkflowRepository projectInstanceWorkflowRepository) {

        this.projectInstanceWorkflowConnectionRepository = projectInstanceWorkflowConnectionRepository;
        this.projectInstanceWorkflowJobRepository = projectInstanceWorkflowJobRepository;
        this.projectInstanceWorkflowRepository = projectInstanceWorkflowRepository;
    }

    @Override
    public void addJob(long projectInstanceWorkflowId, long jobId) {
        ProjectInstanceWorkflowJob projectInstanceWorkflowJob = new ProjectInstanceWorkflowJob(
            projectInstanceWorkflowId, jobId);

        projectInstanceWorkflowJobRepository.save(projectInstanceWorkflowJob);
    }

    @Override
    public List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        return projectInstanceWorkflowRepository.saveAll(projectInstanceWorkflows);
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(long id) {
        return OptionalUtils.get(projectInstanceWorkflowRepository.findById(id));
    }

    @Override
    public ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(String key, String taskName) {
        return OptionalUtils.get(projectInstanceWorkflowConnectionRepository.findByKeyAndTaskName(key, taskName));
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(String workflowId, long projectInstanceId) {
        return projectInstanceWorkflowRepository.findByProjectInstanceIdAndWorkflowId(projectInstanceId, workflowId);
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId) {
        return projectInstanceWorkflowRepository.findAllByProjectInstanceId(projectInstanceId);
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds) {
        return projectInstanceWorkflowRepository.findAllByProjectInstanceIdIn(projectInstanceIds);
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        Assert.notNull(projectInstanceWorkflows, "'projectInstanceWorkflows' must not be null");

        List<ProjectInstanceWorkflow> updatedProjectInstanceWorkflows = new ArrayList<>();

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            ProjectInstanceWorkflow curProjectInstanceWorkflow = OptionalUtils.get(
                projectInstanceWorkflowRepository.findById(projectInstanceWorkflow.getId()));

            curProjectInstanceWorkflow.setConnections(projectInstanceWorkflow.getConnections());
            curProjectInstanceWorkflow.setEnabled(projectInstanceWorkflow.isEnabled());
            curProjectInstanceWorkflow.setInputs(projectInstanceWorkflow.getInputs());
            curProjectInstanceWorkflow.setVersion(projectInstanceWorkflow.getVersion());

            updatedProjectInstanceWorkflows.add(projectInstanceWorkflowRepository.save(curProjectInstanceWorkflow));
        }

        return updatedProjectInstanceWorkflows;
    }
}
