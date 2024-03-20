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
import com.bytechef.atlas.configuration.repository.git.config.GitWorkflowRepositoryProperties;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private final Map<Integer, GitWorkflowOperations> gitWorkflowOperationsMap;

    public GitWorkflowRepository(int type, GitWorkflowOperations gitWorkflowOperationsMap) {
        this.gitWorkflowOperationsMap = Map.of(type, gitWorkflowOperationsMap);
    }

    public GitWorkflowRepository(Map<Integer, GitWorkflowRepositoryProperties> gitWorkflowRepositoryPropertiesMap) {
        this.gitWorkflowOperationsMap = MapUtils.toMap(
            gitWorkflowRepositoryPropertiesMap,
            Map.Entry::getKey,
            entry -> {
                GitWorkflowRepositoryProperties gitWorkflowRepositoryProperties = entry.getValue();

                return new JGitWorkflowOperations(
                    gitWorkflowRepositoryProperties.url(), gitWorkflowRepositoryProperties.branch(),
                    List.of("yaml", "yml"), Arrays.asList(gitWorkflowRepositoryProperties.searchPaths()),
                    gitWorkflowRepositoryProperties.username(), gitWorkflowRepositoryProperties.password());
            });
    }

    @Override
    public List<Workflow> findAll(int type) {
        synchronized (this) {
            GitWorkflowOperations gitWorkflowOperations = gitWorkflowOperationsMap.get(type);

            List<WorkflowResource> resources = gitWorkflowOperations.getHeadFiles();

            return resources.stream()
                .map(resource -> readWorkflow(resource, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        synchronized (this) {
            return gitWorkflowOperationsMap.keySet()
                .stream()
                .map(type -> {
                    Workflow workflow = null;

                    GitWorkflowOperations gitWorkflowOperations = gitWorkflowOperationsMap.get(type);

                    String fileId = decode(id);

                    if (fileId != null) {
                        WorkflowResource workflowResource = gitWorkflowOperations.getFile(fileId);

                        if (workflowResource != null) {
                            workflow = readWorkflow(workflowResource, type);
                        }
                    }

                    return workflow;
                })
                .filter(Objects::nonNull)
                .findFirst();
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.GIT;
    }

    private static Workflow readWorkflow(WorkflowResource workflowResource, int type) {
        Workflow workflow = null;

        try {
            workflow = WorkflowReader.readWorkflow(workflowResource, type);

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
