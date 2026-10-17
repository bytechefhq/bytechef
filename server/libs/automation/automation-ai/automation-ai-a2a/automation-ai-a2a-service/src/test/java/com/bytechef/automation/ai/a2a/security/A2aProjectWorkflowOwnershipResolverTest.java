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

package com.bytechef.automation.ai.a2a.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2aProjectWorkflowOwnershipResolverTest {

    private final A2aProjectRepository a2aProjectRepository = mock(A2aProjectRepository.class);
    private final A2aProjectWorkflowRepository a2aProjectWorkflowRepository =
        mock(A2aProjectWorkflowRepository.class);
    private final A2aServerRepository a2aServerRepository = mock(A2aServerRepository.class);
    private final A2aProjectWorkflowOwnershipResolver resolver = new A2aProjectWorkflowOwnershipResolver(
        a2aProjectRepository, a2aProjectWorkflowRepository, a2aServerRepository);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("A2aProjectWorkflow");
    }

    @Test
    void testResolvesWorkspaceViaProjectAndServer() {
        A2aProjectWorkflow a2aProjectWorkflow = mock(A2aProjectWorkflow.class);

        when(a2aProjectWorkflow.getA2aProjectId()).thenReturn(5L);
        when(a2aProjectWorkflowRepository.findById(1L)).thenReturn(Optional.of(a2aProjectWorkflow));

        A2aProject a2aProject = mock(A2aProject.class);

        when(a2aProject.getA2aServerId()).thenReturn(7L);
        when(a2aProjectRepository.findById(5L)).thenReturn(Optional.of(a2aProject));

        A2aServer a2aServer = mock(A2aServer.class);

        when(a2aServer.getWorkspaceId()).thenReturn(42L);
        when(a2aServerRepository.findById(7L)).thenReturn(Optional.of(a2aServer));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownIsUnknown() {
        when(a2aProjectWorkflowRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }
}
