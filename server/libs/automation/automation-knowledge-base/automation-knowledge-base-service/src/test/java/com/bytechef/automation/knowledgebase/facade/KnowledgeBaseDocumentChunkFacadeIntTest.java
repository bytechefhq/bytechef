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

package com.bytechef.automation.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link KnowledgeBaseDocumentChunkFacade}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseDocumentChunkFacadeIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseFileStorage knowledgeBaseFileStorage;

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
    void testGetKnowledgeBaseDocumentChunksByDocumentId() {
        KnowledgeBaseDocumentChunk chunk1 = createChunkWithContent("vector-1");
        KnowledgeBaseDocumentChunk chunk2 = createChunkWithContent("vector-2");

        knowledgeBaseDocumentChunkRepository.save(chunk1);
        knowledgeBaseDocumentChunkRepository.save(chunk2);

        when(knowledgeBaseFileStorage.readChunkContent(chunk1.getContent())).thenReturn("Chunk 1 content");
        when(knowledgeBaseFileStorage.readChunkContent(chunk2.getContent())).thenReturn("Chunk 2 content");

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(document.getId());

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allMatch(chunk -> chunk.getTextContent() != null);
    }

    @Test
    void testGetKnowledgeBaseDocumentChunksByDocumentIdWithNullContent() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("vector-store-id");

        knowledgeBaseDocumentChunkRepository.save(chunk);

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(document.getId());

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst()
            .getTextContent()).isNull();
    }

    @Test
    void testUpdateKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("test-vector-store-id");

        FileEntry oldContentFileEntry = new FileEntry("old-content.txt", "file://test/chunks/old-content.txt");

        chunk.setContent(oldContentFileEntry);

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        String newContent = "Updated content";
        FileEntry newContentFileEntry = new FileEntry("new-content.txt", "file://test/chunks/new-content.txt");

        when(knowledgeBaseFileStorage.storeChunkContent(anyLong(), eq(newContent))).thenReturn(newContentFileEntry);

        KnowledgeBaseDocumentChunk updatedChunk =
            knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(chunk.getId(), newContent);

        assertThat(updatedChunk.getTextContent()).isEqualTo(newContent);
        assertThat(updatedChunk.getContent()).isEqualTo(newContentFileEntry);

        verify(knowledgeBaseFileStorage).deleteChunkContent(oldContentFileEntry);
        verify(knowledgeBaseFileStorage).storeChunkContent(chunk.getId(), newContent);
    }

    @Test
    void testUpdateKnowledgeBaseDocumentChunkWithoutExistingContent() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("test-vector-store-id");

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        String newContent = "New content";
        FileEntry newContentFileEntry = new FileEntry("new-content.txt", "file://test/chunks/new-content.txt");

        when(knowledgeBaseFileStorage.storeChunkContent(anyLong(), eq(newContent))).thenReturn(newContentFileEntry);

        KnowledgeBaseDocumentChunk updatedChunk =
            knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(chunk.getId(), newContent);

        assertThat(updatedChunk.getTextContent()).isEqualTo(newContent);
        assertThat(updatedChunk.getContent()).isEqualTo(newContentFileEntry);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("test-vector-store-id");

        FileEntry contentFileEntry = new FileEntry("content.txt", "file://test/chunks/content.txt");

        chunk.setContent(contentFileEntry);

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        Long chunkId = chunk.getId();

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunkId)).isPresent();

        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(chunkId);

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunkId)).isNotPresent();

        verify(knowledgeBaseFileStorage).deleteChunkContent(contentFileEntry);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunkWithoutContent() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("test-vector-store-id");

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        Long chunkId = chunk.getId();

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunkId)).isPresent();

        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(chunkId);

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunkId)).isNotPresent();
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunkWithoutVectorStoreId() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());

        FileEntry contentFileEntry = new FileEntry("content.txt", "file://test/chunks/content.txt");

        chunk.setContent(contentFileEntry);

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        Long chunkId = chunk.getId();

        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(chunkId);

        assertThat(knowledgeBaseDocumentChunkRepository.findById(chunkId)).isNotPresent();

        verify(knowledgeBaseFileStorage).deleteChunkContent(contentFileEntry);
    }

    private KnowledgeBaseDocumentChunk createChunkWithContent(String vectorStoreId) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId(vectorStoreId);
        chunk.setContent(new FileEntry(vectorStoreId + ".txt", "file://test/chunks/" + vectorStoreId + ".txt"));

        return chunk;
    }
}
