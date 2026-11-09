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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.PlatformType;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ClaudeCodeWorkspaceResolverTest {

    @TempDir
    private Path basedir;

    @Test
    void testResolveGivesADeployedWorkflowTheSameDirectoryOnEveryRun() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(
            basedir.toString(), 0);

        Path firstWorkspace = claudeCodeWorkspaceResolver.resolve(deployedContext(11L), null);
        Path secondWorkspace = claudeCodeWorkspaceResolver.resolve(deployedContext(12L), "");

        assertThat(firstWorkspace)
            .isEqualTo(basedir.resolve("public/automation/7/production/42/default"))
            .isDirectory();
        assertThat(secondWorkspace).isEqualTo(firstWorkspace);
        assertThat(claudeCodeWorkspaceResolver.resolve(deployedContext(11L), "review"))
            .isEqualTo(basedir.resolve("public/automation/7/production/42/review"));
    }

    @Test
    void testResolveGivesEachEditorTestRunItsOwnWorkspace() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(
            basedir.toString(), 0);

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getJobId()).thenReturn(5L);

        assertThat(claudeCodeWorkspaceResolver.resolve(actionContextAware, null))
            .isEqualTo(basedir.resolve("public/test/job-5/default"))
            .isDirectory();
    }

    @Test
    void testGetQuotaUsesTheConfiguredMaximumSize() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(
            basedir.toString(), 1024);

        Path workspace = claudeCodeWorkspaceResolver.resolve(deployedContext(11L), null);

        assertThat(claudeCodeWorkspaceResolver.getQuota(workspace))
            .satisfies(quota -> {
                assertThat(quota.root()).isEqualTo(workspace);
                assertThat(quota.maxBytes()).isEqualTo(1024);
            });
    }

    @Test
    void testResolveFailsWithoutABaseDirectoryOrAWorkflow() {
        assertThatThrownBy(() -> new ClaudeCodeWorkspaceResolver(" ", 0).resolve(deployedContext(11L), null))
            .hasMessageContaining("bytechef.component.claude-code.workspace.basedir");
        assertThatThrownBy(
            () -> new ClaudeCodeWorkspaceResolver(basedir.toString(), 0).resolve(mock(ActionContext.class), null))
                .hasMessageContaining("cannot be identified");
    }

    private static ActionContextAware deployedContext(long jobId) {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getEnvironmentId()).thenReturn(2L);
        when(actionContextAware.getJobId()).thenReturn(jobId);
        when(actionContextAware.getJobPrincipalId()).thenReturn(7L);
        when(actionContextAware.getJobPrincipalWorkflowId()).thenReturn(42L);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContextAware.getWorkflowId()).thenReturn("workflow-uuid");

        return actionContextAware;
    }
}
