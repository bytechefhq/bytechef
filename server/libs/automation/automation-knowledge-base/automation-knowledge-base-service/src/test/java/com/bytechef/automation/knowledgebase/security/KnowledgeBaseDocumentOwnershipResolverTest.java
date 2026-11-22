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

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentOwnershipResolverTest {

    private static final long CHUNK_ID = 5L;
    private static final long DOCUMENT_ID = 9L;
    private static final long KNOWLEDGE_BASE_ID = 3L;
    private static final long WORKSPACE_ID = 42L;

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService =
        mock(WorkspaceKnowledgeBaseService.class);
    private final KnowledgeBaseDocumentOwnershipResolver documentResolver =
        new KnowledgeBaseDocumentOwnershipResolver(knowledgeBaseDocumentService, workspaceKnowledgeBaseService);
    private final KnowledgeBaseDocumentChunkOwnershipResolver chunkResolver =
        new KnowledgeBaseDocumentChunkOwnershipResolver(knowledgeBaseDocumentChunkService, documentResolver);

    @Test
    void testResourceTypesMatchTheTokensTheGuardsName() {
        assertThat(documentResolver.resourceType()).isEqualTo("KnowledgeBaseDocument");
        assertThat(chunkResolver.resourceType()).isEqualTo("KnowledgeBaseDocumentChunk");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOwningTheDocumentsKnowledgeBase() {
        stubDocumentInWorkspace();

        assertThat(documentResolver.resolveOwner(DOCUMENT_ID)).isEqualTo(ResourceOwner.ofWorkspace(WORKSPACE_ID));
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOwningTheChunksDocument() {
        stubDocumentInWorkspace();

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = mock(KnowledgeBaseDocumentChunk.class);

        when(knowledgeBaseDocumentChunk.getKnowledgeBaseDocumentId()).thenReturn(DOCUMENT_ID);
        when(knowledgeBaseDocumentChunkService.fetchKnowledgeBaseDocumentChunk(CHUNK_ID))
            .thenReturn(Optional.of(knowledgeBaseDocumentChunk));

        assertThat(chunkResolver.resolveOwner(CHUNK_ID)).isEqualTo(ResourceOwner.ofWorkspace(WORKSPACE_ID));
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheDocumentDoesNotExist() {
        when(knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(DOCUMENT_ID)).thenReturn(Optional.empty());

        assertThat(documentResolver.resolveOwner(DOCUMENT_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheDocumentHasNoKnowledgeBase() {
        when(knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(DOCUMENT_ID))
            .thenReturn(Optional.of(new KnowledgeBaseDocument()));

        assertThat(documentResolver.resolveOwner(DOCUMENT_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheKnowledgeBaseIsAssignedToNoWorkspace() {
        KnowledgeBaseDocument knowledgeBaseDocument = mock(KnowledgeBaseDocument.class);

        when(knowledgeBaseDocument.getKnowledgeBaseId()).thenReturn(KNOWLEDGE_BASE_ID);
        when(knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(DOCUMENT_ID))
            .thenReturn(Optional.of(knowledgeBaseDocument));
        when(workspaceKnowledgeBaseService.fetchWorkspaceId(KNOWLEDGE_BASE_ID)).thenReturn(Optional.empty());

        assertThat(documentResolver.resolveOwner(DOCUMENT_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheChunkDoesNotExist() {
        when(knowledgeBaseDocumentChunkService.fetchKnowledgeBaseDocumentChunk(CHUNK_ID)).thenReturn(Optional.empty());

        assertThat(chunkResolver.resolveOwner(CHUNK_ID)).isEqualTo(ResourceOwner.unknown());
    }

    private void stubDocumentInWorkspace() {
        KnowledgeBaseDocument knowledgeBaseDocument = mock(KnowledgeBaseDocument.class);

        when(knowledgeBaseDocument.getKnowledgeBaseId()).thenReturn(KNOWLEDGE_BASE_ID);
        when(knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(DOCUMENT_ID))
            .thenReturn(Optional.of(knowledgeBaseDocument));
        when(workspaceKnowledgeBaseService.fetchWorkspaceId(KNOWLEDGE_BASE_ID)).thenReturn(Optional.of(WORKSPACE_ID));
    }
}
