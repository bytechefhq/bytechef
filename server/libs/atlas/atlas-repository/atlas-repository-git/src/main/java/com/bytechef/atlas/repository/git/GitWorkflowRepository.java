
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.repository.git;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.repository.git.workflow.GitWorkflowOperations;
import com.bytechef.atlas.repository.git.workflow.JGitWorkflowOperations;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowResource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Arik Cohen
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private final GitWorkflowOperations gitWorkflowOperations;
    private final WorkflowMapper workflowMapper;

    public GitWorkflowRepository(GitWorkflowOperations aGitWorkflowOperations, WorkflowMapper workflowMapper) {
        gitWorkflowOperations = aGitWorkflowOperations;
        this.workflowMapper = workflowMapper;
    }

    public GitWorkflowRepository(
        String url,
        String branch,
        String[] searchPaths,
        String username,
        String password,
        WorkflowMapper workflowMapper) {
        gitWorkflowOperations = new JGitWorkflowOperations(url, branch, searchPaths, username, password);
        this.workflowMapper = workflowMapper;
    }

    @Override
    public Iterable<Workflow> findAll() {
        synchronized (this) {
            List<WorkflowResource> resources = gitWorkflowOperations.getHeadFiles();

            return resources.stream()
                .map(workflowMapper::readValue)
                .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        synchronized (this) {
            WorkflowResource resource = gitWorkflowOperations.getFile(id);

            return Optional.of(workflowMapper.readValue(resource));
        }
    }

    @Override
    public Workflow.SourceType getSourceType() {
        return Workflow.SourceType.GIT;
    }
}
