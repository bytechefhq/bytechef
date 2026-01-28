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

package com.bytechef.automation.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link KnowledgeBaseDocumentChunkService}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseDocumentChunkServiceIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private KnowledgeBase knowledgeBase;
    private KnowledgeBaseDocument document;

    @BeforeEach
    public void beforeEach() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

        document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName("Test Document");
        document.setDocument(new FileEntry("test.txt", "file://test/test.txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);

        document = knowledgeBaseDocumentRepository.save(document);
    }

    @AfterEach
    public void afterEach() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testSaveKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = createChunk("vector-store-id-1");

        KnowledgeBaseDocumentChunk savedChunk = knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(chunk);

        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getKnowledgeBaseDocumentId()).isEqualTo(document.getId());
        assertThat(savedChunk.getVectorStoreId()).isEqualTo("vector-store-id-1");
    }

    @Test
    void testGetKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-1"));

        KnowledgeBaseDocumentChunk retrievedChunk =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(chunk.getId());

        assertThat(retrievedChunk).isNotNull();
        assertThat(retrievedChunk.getId()).isEqualTo(chunk.getId());
        assertThat(retrievedChunk.getVectorStoreId()).isEqualTo("vector-store-id-1");
    }

    @Test
    void testGetKnowledgeBaseDocumentChunkNotFound() {
        assertThatThrownBy(() -> knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(Long.MAX_VALUE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("KnowledgeBase document chunk not found");
    }

    @Test
    void testGetKnowledgeBaseDocumentChunkByVectorStoreId() {
        KnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkRepository.save(
            createChunk("unique-vector-store-id"));

        Optional<KnowledgeBaseDocumentChunk> retrievedChunk =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunkByVectorStoreId("unique-vector-store-id");

        assertThat(retrievedChunk).isPresent();
        assertThat(retrievedChunk.get()
            .getId()).isEqualTo(chunk.getId());
    }

    @Test
    void testGetKnowledgeBaseDocumentChunkByVectorStoreIdNotFound() {
        Optional<KnowledgeBaseDocumentChunk> retrievedChunk =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunkByVectorStoreId("non-existent-id");

        assertThat(retrievedChunk).isNotPresent();
    }

    @Test
    void testGetKnowledgeBaseDocumentChunksByDocumentId() {
        knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-1"));
        knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-2"));
        knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-3"));

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(document.getId());

        assertThat(chunks).hasSize(3);
    }

    @Test
    void testGetKnowledgeBaseDocumentChunks() {
        KnowledgeBaseDocumentChunk chunk1 = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-1"));
        KnowledgeBaseDocumentChunk chunk2 = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-2"));

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunks(List.of(chunk1.getId(), chunk2.getId()));

        assertThat(chunks).hasSize(2);
    }

    @Test
    void testSaveKnowledgeBaseDocumentChunks() {
        KnowledgeBaseDocumentChunk chunk1 = createChunk("vector-store-id-1");
        KnowledgeBaseDocumentChunk chunk2 = createChunk("vector-store-id-2");

        List<KnowledgeBaseDocumentChunk> savedChunks =
            knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunks(List.of(chunk1, chunk2));

        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks).allMatch(chunk -> chunk.getId() != null);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-1"));

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunk.getId())).isPresent();

        knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunk(chunk.getId());

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunk.getId())).isNotPresent();
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunks() {
        KnowledgeBaseDocumentChunk chunk1 = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-1"));
        KnowledgeBaseDocumentChunk chunk2 = knowledgeBaseDocumentChunkRepository.save(createChunk("vector-store-id-2"));

        assertThat(knowledgeBaseDocumentChunkRepository.findAll()).hasSize(2);

        knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunks(List.of(chunk1, chunk2));

        assertThat(knowledgeBaseDocumentChunkRepository.findAll()).isEmpty();
    }

    private KnowledgeBaseDocumentChunk createChunk(String vectorStoreId) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId(vectorStoreId);

        return chunk;
    }
}
