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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceLayoutTest {

    @TempDir
    private Path basedir;

    @Test
    void testResolveGivesADeployedWorkflowADirectoryPerDeploymentEnvironmentAndWorkflow() {
        Path workspace = AgentWorkspaceLayout.resolve(basedir, deployedContext(), null);

        assertThat(workspace)
            .isEqualTo(basedir.resolve("public/embedded/7/staging/42/default"))
            .isDirectory();
        assertThat(AgentWorkspaceLayout.resolve(basedir, deployedContext(), "review"))
            .isEqualTo(basedir.resolve("public/embedded/7/staging/42/review"));
    }

    @Test
    void testResolveSeparatesTenants() {
        Path workspace = TenantContext.callWithTenantId(
            "tenant.one", () -> AgentWorkspaceLayout.resolve(basedir, deployedContext(), null));

        assertThat(workspace).isEqualTo(basedir.resolve("tenant_one/embedded/7/staging/42/default"));
    }

    @Test
    void testResolveGivesEachTestRunItsOwnDirectory() {
        ActionContextAware withJob = mock(ActionContextAware.class);
        ActionContextAware withoutJob = mock(ActionContextAware.class);

        when(withJob.getJobId()).thenReturn(5L);
        when(withoutJob.getWorkflowId()).thenReturn("../a1b2:c3");

        assertThat(AgentWorkspaceLayout.resolve(basedir, withJob, null))
            .isEqualTo(basedir.resolve("public/test/job-5/default"));
        assertThat(AgentWorkspaceLayout.resolve(basedir, withoutJob, null))
            .isEqualTo(basedir.resolve("public/test/workflow-___a1b2_c3/default"));
    }

    @Test
    void testResolveRejectsAnInvalidWorkspaceName() {
        assertThatThrownBy(() -> AgentWorkspaceLayout.resolve(basedir, deployedContext(), "../other"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("../other");
    }

    private static ActionContextAware deployedContext() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getEnvironmentId()).thenReturn(1L);
        when(actionContextAware.getJobId()).thenReturn(11L);
        when(actionContextAware.getJobPrincipalId()).thenReturn(7L);
        when(actionContextAware.getJobPrincipalWorkflowId()).thenReturn(42L);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.EMBEDDED);

        return actionContextAware;
    }
}
