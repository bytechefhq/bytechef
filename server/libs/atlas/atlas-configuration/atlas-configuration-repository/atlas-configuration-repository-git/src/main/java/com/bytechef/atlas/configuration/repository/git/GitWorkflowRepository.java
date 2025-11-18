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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.configuration.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.GitInfo;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.HeadFiles;
import com.bytechef.atlas.configuration.repository.git.operations.JGitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class GitWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(GitWorkflowRepository.class);

    private static final ConcurrentHashMap<String, ReentrantLock> TENANT_LOCKS = new ConcurrentHashMap<>();

    private final GitWorkflowOperations gitWorkflowOperations;

    @SuppressFBWarnings("EI")
    public GitWorkflowRepository(GitWorkflowOperations gitWorkflowOperations) {
        this.gitWorkflowOperations = gitWorkflowOperations;
    }

    @SuppressFBWarnings("EI")
    public GitWorkflowRepository(ApplicationProperties applicationProperties) {
        ApplicationProperties.Workflow.Repository.Git git = applicationProperties.getWorkflow()
            .getRepository()
            .getGit();

        this.gitWorkflowOperations = new JGitWorkflowOperations(
            git.getUrl(), git.getBranch(), List.of("json", "yaml", "yml"), Arrays.asList(git.getSearchPaths()),
            git.getUsername(), git.getPassword());
    }

    public GitWorkflowRepository(String url, String branch, String username, String password) {
        this.gitWorkflowOperations = new JGitWorkflowOperations(
            url, branch, List.of("json", "yaml", "yml"), List.of(), username, password);
    }

    @Override
    public List<Workflow> findAll() {
        ReentrantLock lock = getTenantLock();
        try {
            lock.lock();

            HeadFiles headFiles = gitWorkflowOperations.getHeadFiles();

            return headFiles.workflowResources()
                .stream()
                .map(GitWorkflowRepository::readWorkflow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    public GitWorkflows findAllWithGitInfo() {
        ReentrantLock lock = getTenantLock();
        try {
            lock.lock();

            HeadFiles headFiles = gitWorkflowOperations.getHeadFiles();

            return new GitWorkflows(
                headFiles.workflowResources()
                    .stream()
                    .map(GitWorkflowRepository::readWorkflow)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()),
                headFiles.gitInfo());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        ReentrantLock lock = getTenantLock();
        try {
            lock.lock();

            Workflow workflow = null;

            String fileId = decode(id);

            if (fileId != null) {
                WorkflowResource workflowResource = gitWorkflowOperations.getFile(fileId);

                if (workflowResource != null) {
                    workflow = readWorkflow(workflowResource);
                }
            }

            return Optional.ofNullable(workflow);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.GIT;
    }

    public List<String> getRemoteBranches() {
        return gitWorkflowOperations.getRemoteBranches();
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

    public String save(List<Workflow> workflows, String commitMessage) {
        return gitWorkflowOperations.write(
            workflows.stream()
                .map(workflow -> new WorkflowResource(
                    workflow.getId(), Map.of(), getResource(workflow), workflow.getFormat()))
                .toList(),
            commitMessage);
    }

    private static ByteArrayResource getResource(Workflow workflow) {
        String definition = workflow.getDefinition();

        return new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)) {

            @Override
            public String getFilename() {
                Workflow.Format format = workflow.getFormat();

                String name = format.name();

                return workflow.getLabel() + "." + name.toLowerCase();
            }
        };
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

    private ReentrantLock getTenantLock() {
        String tenantKey = TenantCacheKeyUtils.getKey("git-workflow");
        return TENANT_LOCKS.computeIfAbsent(tenantKey, k -> new ReentrantLock());
    }

    @SuppressFBWarnings("EI")
    public record GitWorkflows(List<Workflow> workflows, GitInfo gitInfo) {
    }
}
