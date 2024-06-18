/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.configuration.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.atlas.configuration.repository.git.config.GitWorkflowRepositoryProperties;
import com.bytechef.ee.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.ee.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(GitWorkflowRepository.class);

    private final GitWorkflowOperations gitWorkflowOperations;

    public GitWorkflowRepository(GitWorkflowOperations gitWorkflowOperations) {
        this.gitWorkflowOperations = gitWorkflowOperations;
    }

    public GitWorkflowRepository(GitWorkflowRepositoryProperties gitWorkflowRepositoryProperties) {
        this.gitWorkflowOperations = new JGitWorkflowOperations(
            gitWorkflowRepositoryProperties.url(), gitWorkflowRepositoryProperties.branch(),
            List.of("yaml", "yml"), Arrays.asList(gitWorkflowRepositoryProperties.searchPaths()),
            gitWorkflowRepositoryProperties.username(), gitWorkflowRepositoryProperties.password());
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
            return EncodingUtils.decodeBase64ToString(str);
        } catch (IllegalArgumentException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage());
            }
        }

        return null;
    }

    private static String encode(String id) {
        return EncodingUtils.encodeBase64ToString(id);
    }
}
