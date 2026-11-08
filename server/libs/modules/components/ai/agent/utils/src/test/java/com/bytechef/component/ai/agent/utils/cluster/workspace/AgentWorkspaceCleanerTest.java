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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.service.TenantService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceCleanerTest {

    private static final Duration IDLE_RETENTION = Duration.ofDays(30);
    private static final Duration TEST_RETENTION = Duration.ofHours(24);

    @TempDir
    private Path basedir;

    @TempDir
    private Path outsideDirectory;

    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final TenantService tenantService = mock(TenantService.class);

    @BeforeEach
    void beforeEach() {
        when(jobPrincipalAccessor.getType()).thenReturn(PlatformType.AUTOMATION);
        when(jobPrincipalAccessor.isJobPrincipalPresent(anyLong())).thenReturn(true);
        when(jobPrincipalAccessor.isJobPrincipalWorkflowPresent(anyLong(), anyLong())).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of("tenant_one"));
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
    }

    @Test
    void testCleanRemovesExpiredTestWorkspacesOfEveryTenant() throws IOException {
        Path expiredTestWorkspace = createDirectory("tenant_one/test/job-1/default", olderThan(TEST_RETENTION));
        Path otherTenantExpiredTestWorkspace = createDirectory(
            "tenant_two/test/job-2/default", olderThan(TEST_RETENTION));
        Path recentTestWorkspace = createDirectory("tenant_one/test/job-3/default", Instant.now());

        createCleaner(null, null, null).clean();

        assertThat(expiredTestWorkspace.getParent()).doesNotExist();
        assertThat(otherTenantExpiredTestWorkspace.getParent()).doesNotExist();
        assertThat(recentTestWorkspace).isDirectory();
    }

    @Test
    void testCleanRemovesTheWorkspacesOfADeletedJobPrincipal() throws IOException {
        Path deletedPrincipalWorkspace = createDirectory("tenant_one/automation/11/staging/21/default", Instant.now());
        Path presentPrincipalWorkspace = createDirectory("tenant_one/automation/12/staging/22/default", Instant.now());

        when(jobPrincipalAccessor.isJobPrincipalPresent(11L)).thenReturn(false);

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(basedir.resolve("tenant_one/automation/11")).doesNotExist();
        assertThat(deletedPrincipalWorkspace).doesNotExist();
        assertThat(presentPrincipalWorkspace).isDirectory();
    }

    @Test
    void testCleanRemovesTheWorkspacesOfARemovedJobPrincipalWorkflow() throws IOException {
        Path removedWorkflowWorkspace = createDirectory("tenant_one/automation/11/staging/21/default", Instant.now());
        Path presentWorkflowWorkspace = createDirectory("tenant_one/automation/11/staging/22/default", Instant.now());

        when(jobPrincipalAccessor.isJobPrincipalWorkflowPresent(11L, 21L)).thenReturn(false);

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(removedWorkflowWorkspace.getParent()).doesNotExist();
        assertThat(presentWorkflowWorkspace).isDirectory();
    }

    @Test
    void testCleanKeepsWorkflowDirectoriesThatAreNotNamedByAJobPrincipalWorkflowId() throws IOException {
        Path workspace = createDirectory("tenant_one/automation/11/staging/workflow-abc/default", Instant.now());

        when(jobPrincipalAccessor.isJobPrincipalWorkflowPresent(anyLong(), anyLong())).thenReturn(false);

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(workspace).isDirectory();
    }

    @Test
    void testCleanKeepsTheWorkspacesWhenTheLookupFails() throws IOException {
        Path workspace = createDirectory("tenant_one/automation/11/staging/21/default", Instant.now());

        when(jobPrincipalAccessor.isJobPrincipalPresent(11L)).thenThrow(new IllegalStateException("database down"));

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(workspace).isDirectory();
    }

    @Test
    void testCleanKeepsTheWorkspacesOfAPlatformWithoutAJobPrincipalAccessor() throws IOException {
        Path workspace = createDirectory("tenant_one/embedded/11/staging/21/default", Instant.now());

        when(jobPrincipalAccessor.isJobPrincipalPresent(anyLong())).thenReturn(false);

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(workspace).isDirectory();
    }

    @Test
    void testCleanRemovesTheWorkspacesOfADeletedTenantOnAMultiTenantInstallation() throws IOException {
        Path deletedTenantWorkspace = createDirectory("tenant_two/automation/11/staging/21/default", Instant.now());
        Path presentTenantWorkspace = createDirectory("tenant_one/automation/11/staging/21/default", Instant.now());

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(basedir.resolve("tenant_two")).doesNotExist();
        assertThat(deletedTenantWorkspace).doesNotExist();
        assertThat(presentTenantWorkspace).isDirectory();
    }

    @Test
    void testCleanKeepsUnknownTenantDirectoriesOnASingleTenantInstallation() throws IOException {
        Path unknownTenantWorkspace = createDirectory("tenant_two/automation/11/staging/21/default", Instant.now());

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(unknownTenantWorkspace).isDirectory();
    }

    @Test
    void testCleanKeepsTheTenantDirectoriesWhenTheTenantsCannotBeListed() throws IOException {
        Path workspace = createDirectory("tenant_two/automation/11/staging/21/default", Instant.now());

        when(tenantService.getTenantIds()).thenThrow(new IllegalStateException("database down"));

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(workspace).isDirectory();
    }

    @Test
    void testCleanRemovesIdleWorkspacesWhenAnIdleRetentionIsSet() throws IOException {
        Path idleWorkspace = createDirectory("tenant_one/automation/11/staging/21/default", olderThan(IDLE_RETENTION));
        Path activeWorkspace = createDirectory("tenant_one/automation/11/staging/21/build", olderThan(IDLE_RETENTION));

        Files.writeString(activeWorkspace.resolve("recent.txt"), "recent");

        Files.setLastModifiedTime(activeWorkspace, FileTime.from(olderThan(IDLE_RETENTION)));

        createCleaner(IDLE_RETENTION, tenantService, jobPrincipalAccessor).clean();

        assertThat(idleWorkspace).doesNotExist();
        assertThat(activeWorkspace).isDirectory();
    }

    @Test
    void testCleanKeepsIdleWorkspacesWithoutAnIdleRetention() throws IOException {
        Path idleWorkspace = createDirectory("tenant_one/automation/11/staging/21/default", olderThan(IDLE_RETENTION));

        createCleaner(null, tenantService, jobPrincipalAccessor).clean();

        assertThat(idleWorkspace).isDirectory();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testCleanDoesNotFollowSymbolicLinks() throws IOException {
        Path outsideFile = Files.writeString(outsideDirectory.resolve("keep.txt"), "keep");

        Files.setLastModifiedTime(outsideDirectory, FileTime.from(olderThan(TEST_RETENTION)));

        Path testDirectory = Files.createDirectories(basedir.resolve("tenant_one/test"));

        Files.createSymbolicLink(testDirectory.resolve("job-1"), outsideDirectory);

        createCleaner(null, null, null).clean();

        assertThat(outsideFile).exists();
    }

    @Test
    void testCleanDoesNothingWithoutTheBaseDirectory() {
        Path missingBasedir = basedir.resolve("missing");

        new AgentWorkspaceCleaner(missingBasedir, TEST_RETENTION, null, tenantService, null).clean();

        assertThat(missingBasedir).doesNotExist();
    }

    private AgentWorkspaceCleaner createCleaner(
        Duration idleRetention, TenantService tenantService, JobPrincipalAccessor jobPrincipalAccessor) {

        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = jobPrincipalAccessor == null
            ? null : new JobPrincipalAccessorRegistry(List.of(jobPrincipalAccessor));

        return new AgentWorkspaceCleaner(
            basedir, TEST_RETENTION, idleRetention, tenantService, jobPrincipalAccessorRegistry);
    }

    private Path createDirectory(String relativePath, Instant lastModified) throws IOException {
        Path directory = Files.createDirectories(basedir.resolve(relativePath));

        Path current = basedir;

        for (Path segment : Path.of(relativePath)) {
            current = current.resolve(segment);

            Files.setLastModifiedTime(current, FileTime.from(lastModified));
        }

        return directory;
    }

    private static Instant olderThan(Duration retention) {
        return Instant.now()
            .minus(retention)
            .minusSeconds(60);
    }
}
