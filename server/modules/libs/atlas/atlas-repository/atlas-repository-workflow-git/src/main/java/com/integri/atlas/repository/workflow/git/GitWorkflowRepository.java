/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.repository.workflow.git;

import com.integri.atlas.engine.config.WorkflowRepositoryProperties.GitProperties;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowResource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arik Cohen
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private final GitOperations git;
    private final WorkflowMapper workflowMapper;

    public GitWorkflowRepository(GitOperations aGitOperations, WorkflowMapper workflowMapper) {
        git = aGitOperations;
        this.workflowMapper = workflowMapper;
    }

    public GitWorkflowRepository(GitProperties aGitProperties, WorkflowMapper workflowMapper) {
        git =
            new JGitTemplate(
                aGitProperties.getUrl(),
                aGitProperties.getBranch(),
                aGitProperties.getSearchPaths(),
                aGitProperties.getUsername(),
                aGitProperties.getPassword()
            );
        this.workflowMapper = workflowMapper;
    }

    @Override
    public List<Workflow> findAll() {
        synchronized (this) {
            List<WorkflowResource> resources = git.getHeadFiles();
            List<Workflow> workflows = resources
                .stream()
                .map(r -> workflowMapper.readValue(r))
                .collect(Collectors.toList());
            return workflows;
        }
    }

    @Override
    public Workflow findOne(String aId) {
        synchronized (this) {
            WorkflowResource resource = git.getFile(aId);
            return workflowMapper.readValue(resource);
        }
    }
}
