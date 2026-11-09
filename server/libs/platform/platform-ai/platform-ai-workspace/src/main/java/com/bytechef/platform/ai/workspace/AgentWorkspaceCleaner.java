/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.ai.workspace;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

/**
 * @author Ivica Cardic
 */
public class AgentWorkspaceCleaner {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkspaceCleaner.class);

    private static final Pattern ID_PATTERN = Pattern.compile("[1-9][0-9]{0,17}");

    private final Path basedir;
    private final @Nullable Duration idleRetention;
    private final Predicate<Path> inUse;
    private final @Nullable JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final @Nullable TenantService tenantService;
    private final Duration testRetention;

    @SuppressFBWarnings("EI")
    public AgentWorkspaceCleaner(
        Path basedir, Duration testRetention, @Nullable Duration idleRetention, @Nullable TenantService tenantService,
        @Nullable JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, Predicate<Path> inUse) {

        this.basedir = basedir.toAbsolutePath()
            .normalize();
        this.idleRetention = idleRetention;
        this.inUse = inUse;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.tenantService = tenantService;
        this.testRetention = testRetention;
    }

    public void clean() {
        if (!Files.isDirectory(basedir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        Instant now = Instant.now();

        Map<String, String> tenantIdsByDirectoryName = getTenantIdsByDirectoryName();

        for (Path tenantDirectory : listDirectories(basedir)) {
            removeExpiredTestWorkspaces(
                tenantDirectory.resolve(AgentWorkspaceLayout.TEST_DIRECTORY), now.minus(testRetention));

            if (tenantIdsByDirectoryName.isEmpty()) {
                continue;
            }

            String tenantId = tenantIdsByDirectoryName.get(getName(tenantDirectory));

            if (tenantId == null) {
                removeDeletedTenantWorkspaces(tenantDirectory);
            } else {
                TenantContext.runWithTenantId(tenantId, () -> removeOrphanedWorkspaces(tenantDirectory, now));
            }
        }
    }

    private Map<String, String> getTenantIdsByDirectoryName() {
        if (tenantService == null || jobPrincipalAccessorRegistry == null) {
            return Map.of();
        }

        Map<String, String> tenantIdsByDirectoryName = new HashMap<>();

        try {
            for (String tenantId : tenantService.getTenantIds()) {
                tenantIdsByDirectoryName.put(AgentWorkspaceLayout.toSegment(tenantId), tenantId);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to list the tenants, keeping the agent workspaces of deployed workflows", exception);

            return Map.of();
        }

        return tenantIdsByDirectoryName;
    }

    private void removeDeletedTenantWorkspaces(Path tenantDirectory) {
        if (tenantService != null && tenantService.isMultiTenantEnabled()) {
            delete(tenantDirectory);
        }
    }

    private void removeExpiredTestWorkspaces(Path testDirectory, Instant expiry) {
        for (Path testWorkspace : listDirectories(testDirectory)) {
            if (isModifiedBefore(testWorkspace, expiry)) {
                delete(testWorkspace);
            }
        }
    }

    private void removeOrphanedWorkspaces(Path tenantDirectory, Instant now) {
        for (PlatformType platformType : PlatformType.values()) {
            Path platformDirectory = tenantDirectory.resolve(AgentWorkspaceLayout.toPlatformSegment(platformType));

            Optional<JobPrincipalAccessor> jobPrincipalAccessor = fetchJobPrincipalAccessor(platformType);

            if (jobPrincipalAccessor.isEmpty()) {
                continue;
            }

            for (Path principalDirectory : listDirectories(platformDirectory)) {
                try {
                    removeOrphanedPrincipalWorkspaces(principalDirectory, jobPrincipalAccessor.get(), now);
                } catch (RuntimeException exception) {
                    log.warn("Failed to check the agent workspaces in {}, keeping them", principalDirectory, exception);
                }
            }
        }
    }

    private void removeOrphanedPrincipalWorkspaces(
        Path principalDirectory, JobPrincipalAccessor jobPrincipalAccessor, Instant now) {

        Long jobPrincipalId = parseId(getName(principalDirectory));

        if (jobPrincipalId == null) {
            return;
        }

        if (!jobPrincipalAccessor.isJobPrincipalPresent(jobPrincipalId)) {
            delete(principalDirectory);

            return;
        }

        for (Path environmentDirectory : listDirectories(principalDirectory)) {
            for (Path workflowDirectory : listDirectories(environmentDirectory)) {
                Long jobPrincipalWorkflowId = parseId(getName(workflowDirectory));

                if (jobPrincipalWorkflowId != null &&
                    !jobPrincipalAccessor.isJobPrincipalWorkflowPresent(jobPrincipalId, jobPrincipalWorkflowId)) {

                    delete(workflowDirectory);

                    continue;
                }

                removeIdleWorkspaces(workflowDirectory, now);
            }
        }
    }

    private void removeIdleWorkspaces(Path workflowDirectory, Instant now) {
        if (idleRetention == null) {
            return;
        }

        Instant expiry = now.minus(idleRetention);

        for (Path workspaceDirectory : listDirectories(workflowDirectory)) {
            if (isTreeModifiedBefore(workspaceDirectory, expiry)) {
                delete(workspaceDirectory);
            }
        }
    }

    private Optional<JobPrincipalAccessor> fetchJobPrincipalAccessor(PlatformType platformType) {
        if (jobPrincipalAccessorRegistry == null) {
            return Optional.empty();
        }

        return jobPrincipalAccessorRegistry.fetchJobPrincipalAccessor(platformType);
    }

    private void delete(Path directory) {
        Path normalizedDirectory = directory.toAbsolutePath()
            .normalize();

        if (!normalizedDirectory.startsWith(basedir) || normalizedDirectory.equals(basedir) ||
            inUse.test(normalizedDirectory)) {

            return;
        }

        try {
            FileSystemUtils.deleteRecursively(normalizedDirectory);
        } catch (IOException ioException) {
            log.warn("Failed to delete the agent workspace {}", normalizedDirectory, ioException);
        }
    }

    private static String getName(Path path) {
        Path fileName = path.getFileName();

        return fileName == null ? "" : fileName.toString();
    }

    private static boolean isModifiedBefore(Path path, Instant expiry) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);

            return lastModifiedTime.toInstant()
                .isBefore(expiry);
        } catch (IOException ioException) {
            return false;
        }
    }

    private static boolean isTreeModifiedBefore(Path directory, Instant expiry) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.allMatch(path -> isModifiedBefore(path, expiry));
        } catch (IOException | UncheckedIOException exception) {
            return false;
        }
    }

    private static List<Path> listDirectories(Path directory) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .toList();
        } catch (IOException | UncheckedIOException exception) {
            log.warn("Failed to list the agent workspaces in {}", directory, exception);

            return List.of();
        }
    }

    private static @Nullable Long parseId(String name) {
        if (!ID_PATTERN.matcher(name)
            .matches()) {

            return null;
        }

        return Long.valueOf(name);
    }
}
