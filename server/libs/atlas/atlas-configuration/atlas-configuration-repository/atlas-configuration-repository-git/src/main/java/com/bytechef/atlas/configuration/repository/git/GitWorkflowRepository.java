
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

package com.bytechef.atlas.configuration.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.EncodingUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Arik Cohen
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(GitWorkflowRepository.class);

    private final GitWorkflowOperations gitWorkflowOperations;

    public GitWorkflowRepository(GitWorkflowOperations gitWorkflowOperations) {
        this.gitWorkflowOperations = gitWorkflowOperations;
    }

    public GitWorkflowRepository(String url, String branch, String[] searchPaths, String username, String password) {
        this.gitWorkflowOperations = new JGitWorkflowOperations(
            url, branch, List.of("yaml", "yml"), Arrays.asList(searchPaths), username, password);
    }

    @Override
    public List<Workflow> findAll() {
        synchronized (this) {
            List<WorkflowResource> resources = gitWorkflowOperations.getHeadFiles();

            return resources.stream()
                .map(GitWorkflowRepository::readWorkflow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        synchronized (this) {
            WorkflowResource resource = gitWorkflowOperations.getFile(decode(id));

            return Optional.ofNullable(resource == null ? null : readWorkflow(resource));
        }
    }

    @Override
    public Workflow.SourceType getSourceType() {
        return Workflow.SourceType.GIT;
    }

    @SuppressFBWarnings("NP")
    private static Workflow readWorkflow(WorkflowResource resource) {
        Workflow workflow = null;

        try {
            workflow = WorkflowReader.readWorkflow(resource);

            workflow.setId(encode(Objects.requireNonNull(workflow.getId())));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return workflow;
    }

    private static String decode(String str) {
        return EncodingUtils.decodeBase64ToString(str);
    }

    private static String encode(String id) {
        return EncodingUtils.encodeBase64ToString(id);
    }
}
