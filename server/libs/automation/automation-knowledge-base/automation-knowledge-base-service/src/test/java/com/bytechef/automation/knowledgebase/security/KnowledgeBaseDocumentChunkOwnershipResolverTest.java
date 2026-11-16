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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentChunkOwnershipResolverTest {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseDocumentOwnershipResolver knowledgeBaseDocumentOwnershipResolver =
        mock(KnowledgeBaseDocumentOwnershipResolver.class);

    private final KnowledgeBaseDocumentChunkOwnershipResolver knowledgeBaseDocumentChunkOwnershipResolver =
        new KnowledgeBaseDocumentChunkOwnershipResolver(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentOwnershipResolver);

    @Test
    void testResourceTypeMatchesThePreAuthorizeToken() {
        assertThat(knowledgeBaseDocumentChunkOwnershipResolver.resourceType())
            .isEqualTo("KnowledgeBaseDocumentChunk");
    }

    @Test
    void testResolveOwnerOfAMissingChunkIsUnknown() {
        when(knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunks(List.of(1303L))).thenReturn(List.of());

        assertThat(knowledgeBaseDocumentChunkOwnershipResolver.resolveOwner(1303L))
            .isEqualTo(ResourceOwner.unknown());

        verifyNoInteractions(knowledgeBaseDocumentOwnershipResolver);
    }

    @Test
    void testResolveOwnerOfAChunkWithoutADocumentIsUnknown() {
        when(knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunks(List.of(1303L)))
            .thenReturn(List.of(new KnowledgeBaseDocumentChunk()));

        assertThat(knowledgeBaseDocumentChunkOwnershipResolver.resolveOwner(1303L))
            .isEqualTo(ResourceOwner.unknown());

        verifyNoInteractions(knowledgeBaseDocumentOwnershipResolver);
    }

    @Test
    void testResolveOwnerReturnsTheOwnerOfTheChunkDocument() {
        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

        knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(1302L);

        when(knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunks(List.of(1303L)))
            .thenReturn(List.of(knowledgeBaseDocumentChunk));
        when(knowledgeBaseDocumentOwnershipResolver.resolveOwner(1302L)).thenReturn(ResourceOwner.ofWorkspace(7L));

        assertThat(knowledgeBaseDocumentChunkOwnershipResolver.resolveOwner(1303L))
            .isEqualTo(ResourceOwner.ofWorkspace(7L));
    }
}
