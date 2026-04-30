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
        "bytechef.ai.knowledge-base.enabled=true",
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
            .isEqualTo("1");
    }

    @Test
    void testGetKnowledgeBaseDocumentChunks() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L),
            createMockChunk(2L));

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

        mockDocument.setTagNames(List.of("Tag 1", "Tag 2"));

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(String.class)
            .hasSize(2);
    }

    @Test
    void testDocumentTagsEmpty() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        mockDocument.setTagNames(List.of());

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(Object.class)
            .hasSize(0);
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
    void testGetKnowledgeBaseDocumentStatus() {
        Long documentId = 1L;
        DocumentStatusUpdate statusUpdate = new DocumentStatusUpdate(documentId, 2, System.currentTimeMillis(), null);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(documentId)).thenReturn(statusUpdate);

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
            .path("knowledgeBaseDocumentStatus.status")
            .entity(Integer.class)
            .isEqualTo(2);
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

    private KnowledgeBaseDocumentChunk createMockChunk(Long id) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setKnowledgeBaseDocumentId(1L);

        return chunk;
    }
}
