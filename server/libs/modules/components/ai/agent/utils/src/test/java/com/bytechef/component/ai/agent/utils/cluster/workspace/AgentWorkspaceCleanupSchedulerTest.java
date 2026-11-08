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
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.tenant.service.TenantService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceCleanupSchedulerTest {

    @TempDir
    private Path basedir;

    private final ApplicationProperties applicationProperties = new ApplicationProperties();

    private Path expiredTestWorkspace;

    @BeforeEach
    void beforeEach() throws IOException {
        expiredTestWorkspace = Files.createDirectories(basedir.resolve("tenant_one/test/job-1"));

        Files.setLastModifiedTime(
            expiredTestWorkspace, FileTime.from(Instant.now()
                .minus(Duration.ofHours(25))));

        ApplicationProperties.Ai.AgentUtils.Workspace workspace = getAgentUtils().getWorkspace();

        workspace.setBasedir(basedir.toString());
    }

    @Test
    void testCleanAgentWorkspacesDoesNothingWhenTheFileAndShellToolsAreDisabled() {
        createScheduler().cleanAgentWorkspaces();

        assertThat(expiredTestWorkspace).isDirectory();
    }

    @Test
    void testCleanAgentWorkspacesRemovesExpiredTestWorkspacesWhenTheFileSystemToolsAreEnabled() {
        ApplicationProperties.Ai.AgentUtils.FileSystem fileSystem = getAgentUtils().getFileSystem();

        fileSystem.setEnabled(true);

        createScheduler().cleanAgentWorkspaces();

        assertThat(expiredTestWorkspace).doesNotExist();
    }

    @Test
    void testCleanAgentWorkspacesRemovesExpiredTestWorkspacesWhenTheShellToolsAreEnabled() {
        ApplicationProperties.Ai.AgentUtils.Shell shell = getAgentUtils().getShell();

        shell.setEnabled(true);

        createScheduler().cleanAgentWorkspaces();

        assertThat(expiredTestWorkspace).doesNotExist();
    }

    @Test
    void testCleanAgentWorkspacesUsesTheConfiguredTestRetention() {
        ApplicationProperties.Ai.AgentUtils agentUtils = getAgentUtils();

        ApplicationProperties.Ai.AgentUtils.FileSystem fileSystem = agentUtils.getFileSystem();
        ApplicationProperties.Ai.AgentUtils.Workspace workspace = agentUtils.getWorkspace();

        fileSystem.setEnabled(true);
        workspace.setTestRetention(Duration.ofDays(2));

        createScheduler().cleanAgentWorkspaces();

        assertThat(expiredTestWorkspace).isDirectory();
    }

    @Test
    void testCleanAgentWorkspacesDoesNothingWithoutABaseDirectory() {
        ApplicationProperties.Ai.AgentUtils agentUtils = getAgentUtils();

        ApplicationProperties.Ai.AgentUtils.FileSystem fileSystem = agentUtils.getFileSystem();
        ApplicationProperties.Ai.AgentUtils.Workspace workspace = agentUtils.getWorkspace();

        fileSystem.setEnabled(true);
        workspace.setBasedir(" ");

        createScheduler().cleanAgentWorkspaces();

        assertThat(expiredTestWorkspace).isDirectory();
    }

    @SuppressWarnings("unchecked")
    private AgentWorkspaceCleanupScheduler createScheduler() {
        return new AgentWorkspaceCleanupScheduler(
            applicationProperties, mock(ObjectProvider.class, invocation -> null),
            (ObjectProvider<TenantService>) mock(ObjectProvider.class, invocation -> null));
    }

    private ApplicationProperties.Ai.AgentUtils getAgentUtils() {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return ai.getAgentUtils();
    }
}
