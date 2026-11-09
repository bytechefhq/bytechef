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

package com.bytechef.component.claude.code.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
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
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(basedir.toString());

        ActionContextAware firstRunContext = deployedContext(11L);
        ActionContextAware secondRunContext = deployedContext(12L);

        Path firstWorkspace = claudeCodeWorkspaceResolver.resolve(firstRunContext, null);
        Path secondWorkspace = claudeCodeWorkspaceResolver.resolve(secondRunContext, "");

        assertThat(firstWorkspace)
            .isEqualTo(basedir.resolve("public/automation/7/production/42/default"))
            .isDirectory();
        assertThat(secondWorkspace).isEqualTo(firstWorkspace);
        assertThat(claudeCodeWorkspaceResolver.resolve(firstRunContext, "review"))
            .isEqualTo(basedir.resolve("public/automation/7/production/42/review"));
    }

    @Test
    void testResolveSeparatesTenants() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(basedir.toString());

        Path workspace = TenantContext.callWithTenantId(
            "acme", () -> claudeCodeWorkspaceResolver.resolve(deployedContext(11L), null));

        assertThat(workspace).isEqualTo(basedir.resolve("acme/automation/7/production/42/default"));
    }

    @Test
    void testResolveGivesEditorTestRunsAWorkspacePerWorkflow() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(basedir.toString());

        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getWorkflowId()).thenReturn("../a1b2:c3");

        assertThat(claudeCodeWorkspaceResolver.resolve(actionContextAware, null))
            .isEqualTo(basedir.resolve("public/test/workflow-___a1b2_c3/default"))
            .isDirectory();
    }

    @Test
    void testResolveRejectsAnInvalidWorkspaceName() {
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver = new ClaudeCodeWorkspaceResolver(basedir.toString());

        assertThatThrownBy(() -> claudeCodeWorkspaceResolver.resolve(deployedContext(11L), "../other"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("../other");
    }

    @Test
    void testResolveFailsWithoutABaseDirectoryOrAWorkflow() {
        assertThatThrownBy(() -> new ClaudeCodeWorkspaceResolver(" ").resolve(deployedContext(11L), null))
            .hasMessageContaining("bytechef.component.claude-code.workspace.basedir");
        assertThatThrownBy(
            () -> new ClaudeCodeWorkspaceResolver(basedir.toString()).resolve(mock(ActionContext.class), null))
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
