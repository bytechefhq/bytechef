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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link KnowledgeBaseDocumentGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseDocumentGraphQlController.class
})
@GraphQlTest(
    controllers = KnowledgeBaseDocumentGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseDocumentGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private TagService tagService;

    @Test
    void testGetKnowledgeBaseDocument() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        name
                        status
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("knowledgeBaseDocument.name")
            .entity(String.class)
            .isEqualTo("Test Document")
            .path("knowledgeBaseDocument.status")
            .entity(Integer.class)
            .isEqualTo(KnowledgeBaseDocument.STATUS_READY);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocument(documentId);
    }

    @Test
    void testGetKnowledgeBaseDocumentStatus() {
        Long documentId = 1L;
        DocumentStatusUpdate mockStatusUpdate =
            DocumentStatusUpdate.of(documentId, KnowledgeBaseDocument.STATUS_PROCESSING);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(documentId)).thenReturn(mockStatusUpdate);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocumentStatus(id: "1") {
                        documentId
                        status
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocumentStatus.documentId")
            .entity(String.class)
            .isEqualTo("1")
            .path("knowledgeBaseDocumentStatus.status")
            .entity(Integer.class)
            .isEqualTo(KnowledgeBaseDocument.STATUS_PROCESSING);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocumentStatus(documentId);
    }

    @Test
    void testDeleteKnowledgeBaseDocument() {
        Long documentId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBaseDocument(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBaseDocument")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(knowledgeBaseDocumentFacade).deleteKnowledgeBaseDocument(documentId);
    }

    @Test
    void testDocumentChunks() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L, "Chunk 1 content"),
            createMockChunk(2L, "Chunk 2 content"));

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);
        when(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(documentId))
            .thenReturn(mockChunks);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        chunks {
                            id
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.chunks")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseDocumentChunkFacade).getKnowledgeBaseDocumentChunksByDocumentId(documentId);
    }

    @Test
    void testDocumentTags() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        mockDocument.setTagIds(List.of(1L, 2L));

        List<Tag> mockTags = List.of(
            new Tag(1L, "Tag 1"),
            new Tag(2L, "Tag 2"));

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);
        when(tagService.getTags(List.of(1L, 2L))).thenReturn(mockTags);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(Object.class)
            .hasSize(2);

        verify(tagService).getTags(List.of(1L, 2L));
    }

    @Test
    void testDocumentTagsEmpty() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        mockDocument.setTagIds(List.of());

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags {
                            id
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(Object.class)
            .hasSize(0);
    }

    private KnowledgeBaseDocument createMockDocument(Long id, String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setKnowledgeBaseId(1L);
        document.setName(name);
        document.setDocument(new FileEntry(name + ".txt", "file://test/" + name + ".txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);
        document.setVersion(1);

        return document;
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id, String content) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setKnowledgeBaseDocumentId(1L);
        chunk.setVectorStoreId("vector-store-id-" + id);
        chunk.setTextContent(content);
        chunk.setVersion(1);

        return chunk;
    }
}
