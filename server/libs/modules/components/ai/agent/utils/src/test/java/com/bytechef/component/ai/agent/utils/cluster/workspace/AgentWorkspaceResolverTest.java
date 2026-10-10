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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.common.workspace.Workspace;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceResolverTest {

    private static final String CLUSTER_ELEMENT_NAME = "fileSystemTools";

    @TempDir
    private Path basedir;

    @Test
    void testResolveForProjectDeploymentUsesTenantOwnerEnvironmentAndWorkflow() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);

        Workspace workspace = resolveForTenant("tenant_one", actionContext, null);

        assertThat(workspace.root()).isEqualTo(
            basedir.resolve("tenant_one/automation/11/staging/21/default"))
            .isDirectory();
    }

    @Test
    void testResolveForIntegrationInstanceUsesTheEmbeddedPlatform() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.EMBEDDED, 5L, 2L, 8L);

        Workspace workspace = resolveForTenant("tenant_one", actionContext, "notes");

        assertThat(workspace.root()).isEqualTo(basedir.resolve("tenant_one/embedded/5/production/8/notes"));
    }

    @Test
    void testResolveSeparatesTenantsWithTheSameOwner() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);

        Workspace firstWorkspace = resolveForTenant("tenant_one", actionContext, null);
        Workspace secondWorkspace = resolveForTenant("tenant_two", actionContext, null);

        assertThat(firstWorkspace.root()).startsWith(basedir.resolve("tenant_one"));
        assertThat(secondWorkspace.root()).startsWith(basedir.resolve("tenant_two"));
    }

    @Test
    void testResolveSeparatesWorkspaceNamesOfTheSameAgent() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);

        Workspace firstWorkspace = resolveForTenant("tenant_one", actionContext, "first");
        Workspace secondWorkspace = resolveForTenant("tenant_one", actionContext, "second");

        assertThat(firstWorkspace.root()).isNotEqualTo(secondWorkspace.root());
    }

    @Test
    void testResolveForTestRunUsesAWorkspacePerJob() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getJobId()).thenReturn(42L);

        Workspace workspace = resolveForTenant("tenant_one", actionContext, null);

        assertThat(workspace.root()).isEqualTo(basedir.resolve("tenant_one/test/job-42/default"));
    }

    @Test
    void testResolveForTestRunWithoutJobUsesASanitizedWorkflowId() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getWorkflowId()).thenReturn("../project:1");

        Workspace workspace = resolveForTenant("tenant_one", actionContext, null);

        assertThat(workspace.root()).isEqualTo(basedir.resolve("tenant_one/test/workflow-___project_1/default"));
    }

    @Test
    void testResolveForTestRunRemovesExpiredTestWorkspaces() throws IOException {
        Path expiredTestWorkspace = Files.createDirectories(basedir.resolve("tenant_one/test/job-1"));
        Path recentTestWorkspace = Files.createDirectories(basedir.resolve("tenant_one/test/job-2"));

        Files.setLastModifiedTime(
            expiredTestWorkspace,
            FileTime.from(Instant.now()
                .minus(AgentWorkspaceResolver.TEST_WORKSPACE_RETENTION)
                .minusSeconds(60)));

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getJobId()).thenReturn(3L);

        resolveForTenant("tenant_one", actionContext, null);

        assertThat(expiredTestWorkspace).doesNotExist();
        assertThat(recentTestWorkspace).isDirectory();
    }

    @Test
    void testResolveFromANestedAgentContextUsesTheParentRun() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);
        ActionContext nestedAgentContext = mock(
            ActionContext.class, withSettings().extraInterfaces(JobContextAware.class));

        when(((JobContextAware) nestedAgentContext).toActionContext("aiAgentUtils", 1, CLUSTER_ELEMENT_NAME, null))
            .thenReturn(actionContext);

        Workspace workspace = resolveForTenant("tenant_one", nestedAgentContext, null);

        assertThat(workspace.root()).isEqualTo(basedir.resolve("tenant_one/automation/11/staging/21/default"));
    }

    @Test
    void testResolveRejectsWorkspaceNamesThatAreNotASingleSafeSegment() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);
        AgentWorkspaceResolver agentWorkspaceResolver = new AgentWorkspaceResolver(basedir);

        for (String workspaceName : new String[] {
            "..", "../other", "a/b", "a\\b", "with space", "x".repeat(65)
        }) {
            assertThatThrownBy(() -> agentWorkspaceResolver.resolve(actionContext, CLUSTER_ELEMENT_NAME, workspaceName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace name");
        }
    }

    @Test
    void testResolveSanitizesTheTenantId() {
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);

        Workspace workspace = resolveForTenant("../escape", actionContext, null);

        assertThat(workspace.root()).isEqualTo(basedir.resolve("___escape/automation/11/staging/21/default"));
    }

    @Test
    void testResolveWithoutAnIdentifiableRunFails() {
        AgentWorkspaceResolver agentWorkspaceResolver = new AgentWorkspaceResolver(basedir);

        assertThatThrownBy(() -> agentWorkspaceResolver.resolve(
            mock(ClusterElementContext.class), CLUSTER_ELEMENT_NAME, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be identified");
    }

    @Test
    void testResolveWithoutABaseDirectoryFails() {
        AgentWorkspaceResolver agentWorkspaceResolver = new AgentWorkspaceResolver(null);
        ActionContextAware actionContext = givenDeployedRun(PlatformType.AUTOMATION, 11L, 1L, 21L);

        assertThatThrownBy(() -> agentWorkspaceResolver.resolve(actionContext, CLUSTER_ELEMENT_NAME, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bytechef.ai.agent-utils.workspace.basedir");
    }

    private static ActionContextAware givenDeployedRun(
        PlatformType platformType, long principalId, long environmentId, long principalWorkflowId) {

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(platformType);
        when(actionContext.getJobPrincipalId()).thenReturn(principalId);
        when(actionContext.getEnvironmentId()).thenReturn(environmentId);
        when(actionContext.getJobPrincipalWorkflowId()).thenReturn(principalWorkflowId);

        return actionContext;
    }

    private Workspace resolveForTenant(
        String tenantId, Context context, String workspaceName) {

        AgentWorkspaceResolver agentWorkspaceResolver = new AgentWorkspaceResolver(basedir);

        return TenantContext.callWithTenantId(
            tenantId, () -> agentWorkspaceResolver.resolve(context, CLUSTER_ELEMENT_NAME, workspaceName));
    }
}
