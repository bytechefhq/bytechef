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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DbBackedAutoMemoryDirectoryOpsTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long PRINCIPAL_ID = 100L;
    private static final int ENVIRONMENT = 0;

    @Test
    void testListUsesFixedProjectDeploymentScoping() {
        AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

        when(
            aiAutoMemoryService.listByPrincipalAndWorkspace(
                WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT))
                    .thenReturn(List.<AiAutoMemory>of());

        DbBackedAutoMemoryDirectoryOps directoryOps =
            new DbBackedAutoMemoryDirectoryOps(
                aiAutoMemoryService, WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID,
                ENVIRONMENT);

        String index = directoryOps.list("MEMORY.md", null);

        assertThat(index).isEqualTo("MEMORY index is empty. Create entries with MemoryCreate.");

        verify(aiAutoMemoryService).listByPrincipalAndWorkspace(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT);
    }

    @Test
    void testDeleteSlugifiesNameAndUsesFixedProjectDeploymentScoping() {
        AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

        DbBackedAutoMemoryDirectoryOps directoryOps =
            new DbBackedAutoMemoryDirectoryOps(
                aiAutoMemoryService, WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID,
                ENVIRONMENT);

        directoryOps.delete("foo", null);

        verify(aiAutoMemoryService).delete(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT, "foo");
    }

    @Test
    void testIntegrationInstancePrincipalTypeIsThreadedThrough() {
        AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

        DbBackedAutoMemoryDirectoryOps directoryOps = new DbBackedAutoMemoryDirectoryOps(
            aiAutoMemoryService, WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, PRINCIPAL_ID,
            ENVIRONMENT);

        directoryOps.delete("foo", null);

        verify(aiAutoMemoryService).delete(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, PRINCIPAL_ID, ENVIRONMENT, "foo");
    }
}
