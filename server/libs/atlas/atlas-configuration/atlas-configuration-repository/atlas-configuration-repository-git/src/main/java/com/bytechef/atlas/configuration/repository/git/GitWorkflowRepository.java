/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.configuration.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(GitWorkflowRepository.class);

    private final GitWorkflowOperations gitWorkflowOperations;

    public GitWorkflowRepository(GitWorkflowOperations gitWorkflowOperations) {
        this.gitWorkflowOperations = gitWorkflowOperations;
    }

    public GitWorkflowRepository(ApplicationProperties applicationProperties) {
        ApplicationProperties.Workflow.Repository.Git git = applicationProperties.getWorkflow()
            .getRepository()
            .getGit();

        this.gitWorkflowOperations = new JGitWorkflowOperations(
            git.getUrl(), git.getBranch(), List.of("yaml", "yml"), Arrays.asList(git.getSearchPaths()),
            git.getUsername(), git.getPassword());
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
            Workflow workflow = null;

            String fileId = decode(id);

            if (fileId != null) {
                WorkflowResource workflowResource = gitWorkflowOperations.getFile(fileId);

                if (workflowResource != null) {
                    workflow = readWorkflow(workflowResource);
                }
            }

            return Optional.ofNullable(workflow);
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.GIT;
    }

    private static Workflow readWorkflow(WorkflowResource workflowResource) {
        Workflow workflow = null;

        try {
            workflow = WorkflowReader.readWorkflow(workflowResource);

            workflow.setId(encode(Validate.notNull(workflow.getId(), "id")));
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage());
            }
        }

        return workflow;
    }

    private static String decode(String str) {
        try {
            return EncodingUtils.base64DecodeToString(str);
        } catch (IllegalArgumentException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage());
            }
        }

        return null;
    }

    private static String encode(String id) {
        return EncodingUtils.base64EncodeToString(id);
    }
}
