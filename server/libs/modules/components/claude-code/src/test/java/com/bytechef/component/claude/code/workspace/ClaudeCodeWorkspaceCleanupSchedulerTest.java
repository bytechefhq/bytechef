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

package com.bytechef.component.claude.code.workspace;

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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
class ClaudeCodeWorkspaceCleanupSchedulerTest {

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

        ApplicationProperties.Component.ClaudeCode claudeCode = getClaudeCode();

        ApplicationProperties.Component.ClaudeCode.Workspace workspace = claudeCode.getWorkspace();

        workspace.setBasedir(basedir.toString());
    }

    @Test
    void testSchedulerIsNotRegisteredWhenClaudeCodeIsDisabled() {
        createApplicationContextRunner()
            .run(context -> assertThat(context).doesNotHaveBean(ClaudeCodeWorkspaceCleanupScheduler.class));
    }

    @Test
    void testSchedulerIsRegisteredWhenClaudeCodeIsEnabled() {
        createApplicationContextRunner()
            .withPropertyValues("bytechef.component.claude-code.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(ClaudeCodeWorkspaceCleanupScheduler.class));
    }

    @Test
    void testCleanClaudeCodeWorkspacesRemovesExpiredTestWorkspaces() {
        createScheduler().cleanClaudeCodeWorkspaces();

        assertThat(expiredTestWorkspace).doesNotExist();
    }

    @Test
    void testCleanClaudeCodeWorkspacesUsesTheConfiguredTestRetention() {
        ApplicationProperties.Component.ClaudeCode claudeCode = getClaudeCode();

        ApplicationProperties.Component.ClaudeCode.Workspace workspace = claudeCode.getWorkspace();

        workspace.setTestRetention(Duration.ofDays(2));

        createScheduler().cleanClaudeCodeWorkspaces();

        assertThat(expiredTestWorkspace).isDirectory();
    }

    @Test
    void testCleanClaudeCodeWorkspacesDoesNothingWithoutABaseDirectory() {
        ApplicationProperties.Component.ClaudeCode claudeCode = getClaudeCode();

        ApplicationProperties.Component.ClaudeCode.Workspace workspace = claudeCode.getWorkspace();

        workspace.setBasedir(" ");

        createScheduler().cleanClaudeCodeWorkspaces();

        assertThat(expiredTestWorkspace).isDirectory();
    }

    private static ApplicationContextRunner createApplicationContextRunner() {
        return new ApplicationContextRunner()
            .withBean(ApplicationProperties.class)
            .withUserConfiguration(ClaudeCodeWorkspaceCleanupScheduler.class);
    }

    @SuppressWarnings("unchecked")
    private ClaudeCodeWorkspaceCleanupScheduler createScheduler() {
        return new ClaudeCodeWorkspaceCleanupScheduler(
            applicationProperties, mock(ObjectProvider.class, invocation -> null),
            (ObjectProvider<TenantService>) mock(ObjectProvider.class, invocation -> null));
    }

    private ApplicationProperties.Component.ClaudeCode getClaudeCode() {
        ApplicationProperties.Component component = applicationProperties.getComponent();

        return component.getClaudeCode();
    }
}
