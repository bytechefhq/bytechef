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

package com.bytechef.automation.knowledgebase.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeBaseOwnershipResolverTest {

    private final WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository =
        mock(WorkspaceKnowledgeBaseRepository.class);
    private final KnowledgeBaseOwnershipResolver resolver =
        new KnowledgeBaseOwnershipResolver(workspaceKnowledgeBaseRepository);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("KnowledgeBase");
    }

    @Test
    void testResolvesWorkspace() {
        WorkspaceKnowledgeBase workspaceKnowledgeBase = mock(WorkspaceKnowledgeBase.class);

        when(workspaceKnowledgeBase.getWorkspaceId()).thenReturn(42L);
        when(workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(1L))
            .thenReturn(List.of(workspaceKnowledgeBase));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).isEmpty();
    }

    @Test
    void testUnknownIsUnknown() {
        when(workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(99L)).thenReturn(List.of());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }
}
