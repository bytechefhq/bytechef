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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link KnowledgeBaseDocumentChunkGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseDocumentChunkGraphQlController.class
})
@GraphQlTest(
    controllers = KnowledgeBaseDocumentChunkGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.knowledge-base.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseDocumentChunkGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Test
    void testUpdateKnowledgeBaseDocumentChunk() {
        Long chunkId = 1L;
        String newContent = "Updated content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, newContent);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(newContent)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Updated content"}
                    ) {
                        id
                        content
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateKnowledgeBaseDocumentChunk.content")
            .entity(String.class)
            .isEqualTo("Updated content");

        verify(knowledgeBaseDocumentChunkFacade).updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(newContent));
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunk() {
        Long chunkId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBaseDocumentChunk(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBaseDocumentChunk")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(knowledgeBaseDocumentChunkFacade).deleteKnowledgeBaseDocumentChunk(chunkId);
    }

    @Test
    void testChunkContentSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test chunk content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test chunk content"}
                    ) {
                        id
                        content
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.content")
            .entity(String.class)
            .isEqualTo("Test chunk content");
    }

    @Test
    void testChunkMetadataSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        mockChunk.setMetadata(Map.of("key1", "value1", "key2", "value2"));

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test content"}
                    ) {
                        id
                        metadata
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.id")
            .entity(String.class)
            .isEqualTo("1");
    }

    @Test
    void testChunkScoreSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        mockChunk.setScore(0.95f);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test content"}
                    ) {
                        id
                        score
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.score")
            .entity(Float.class)
            .isEqualTo(0.95f);
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
