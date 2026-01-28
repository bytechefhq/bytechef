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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
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
 * Integration tests for {@link KnowledgeBaseGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseGraphQlController.class,
    KnowledgeBaseDocumentGraphQlController.class,
    KnowledgeBaseDocumentChunkGraphQlController.class
})
@GraphQlTest(
    controllers = {
        KnowledgeBaseGraphQlController.class,
        KnowledgeBaseDocumentGraphQlController.class,
        KnowledgeBaseDocumentChunkGraphQlController.class
    },
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseFacade knowledgeBaseFacade;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @Test
    void testGetKnowledgeBases() {
        Long workspaceId = 1L;
        List<KnowledgeBase> mockKnowledgeBases = List.of(
            createMockKnowledgeBase(1L, "KnowledgeBase 1"),
            createMockKnowledgeBase(2L, "KnowledgeBase 2"));

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId)).thenReturn(mockKnowledgeBases);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBases(workspaceId: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("knowledgeBases")
            .entityList(Object.class)
            .hasSize(2);

        verify(workspaceKnowledgeBaseFacade).getWorkspaceKnowledgeBases(workspaceId);
    }

    @Test
    void testGetKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Test KnowledgeBase");

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBase(id: "1") {
                        id
                        name
                        description
                    }
                }
                """)
            .execute()
            .path("knowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("knowledgeBase.name")
            .entity(String.class)
            .isEqualTo("Test KnowledgeBase");

        verify(knowledgeBaseService).getKnowledgeBase(knowledgeBaseId);
    }

    @Test
    void testSearchKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        String query = "test query";

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L, "chunk content 1"),
            createMockChunk(2L, "chunk content 2"));

        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), eq(query), any())).thenReturn(mockChunks);

        this.graphQlTester
            .document("""
                query {
                    searchKnowledgeBase(id: "1", query: "test query") {
                        id
                    }
                }
                """)
            .execute()
            .path("searchKnowledgeBase")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseFacade).searchKnowledgeBase(eq(knowledgeBaseId), eq(query), any());
    }

    @Test
    void testCreateKnowledgeBase() {
        Long workspaceId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(1L, "New KnowledgeBase");

        when(workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(any(KnowledgeBase.class), eq(workspaceId)))
            .thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                mutation {
                    createKnowledgeBase(knowledgeBase: {name: "New KnowledgeBase"}, workspaceId: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("createKnowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createKnowledgeBase.name")
            .entity(String.class)
            .isEqualTo("New KnowledgeBase");

        verify(workspaceKnowledgeBaseFacade).createWorkspaceKnowledgeBase(any(KnowledgeBase.class), eq(workspaceId));
    }

    @Test
    void testUpdateKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Updated KnowledgeBase");

        when(knowledgeBaseService.updateKnowledgeBase(eq(knowledgeBaseId), any(KnowledgeBase.class)))
            .thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBase(id: "1", knowledgeBase: {name: "Updated KnowledgeBase"}) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateKnowledgeBase.name")
            .entity(String.class)
            .isEqualTo("Updated KnowledgeBase");

        verify(knowledgeBaseService).updateKnowledgeBase(eq(knowledgeBaseId), any(KnowledgeBase.class));
    }

    @Test
    void testDeleteKnowledgeBase() {
        Long knowledgeBaseId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBase(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBase")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceKnowledgeBaseFacade).deleteWorkspaceKnowledgeBase(knowledgeBaseId);
    }

    @Test
    void testKnowledgeBaseDocuments() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Test KnowledgeBase");

        List<KnowledgeBaseDocument> mockDocuments = List.of(
            createMockDocument(1L, "Document 1"),
            createMockDocument(2L, "Document 2"));

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(mockKnowledgeBase);
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId)).thenReturn(mockDocuments);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBase(id: "1") {
                        id
                        documents {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBase.documents")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocuments(knowledgeBaseId);
    }

    private KnowledgeBase createMockKnowledgeBase(Long id, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setName(name);
        knowledgeBase.setVersion(1);

        return knowledgeBase;
    }

    private KnowledgeBaseDocument createMockDocument(Long id, String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setName(name);
        document.setDocument(new FileEntry(name + ".txt", "file://test/" + name + ".txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);
        document.setVersion(1);

        return document;
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id, String content) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setVectorStoreId("vector-store-id-" + id);
        chunk.setTextContent(content);
        chunk.setVersion(1);

        return chunk;
    }
}
