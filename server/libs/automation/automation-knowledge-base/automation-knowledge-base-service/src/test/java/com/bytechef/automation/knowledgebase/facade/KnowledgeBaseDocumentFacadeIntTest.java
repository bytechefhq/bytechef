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
import static org.mockito.ArgumentMatchers.any;
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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link KnowledgeBaseDocumentFacade}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseDocumentFacadeIntTest {

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseFileStorage knowledgeBaseFileStorage;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    public void beforeEach() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
    }

    @AfterEach
    public void afterEach() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testCreateKnowledgeBaseDocument() {
        String filename = "test-document.txt";
        String contentType = "text/plain";
        String content = "Test document content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        FileEntry mockFileEntry = new FileEntry(filename, "file://test/" + filename);

        when(knowledgeBaseFileStorage.storeDocument(eq(filename), any(InputStream.class))).thenReturn(mockFileEntry);

        KnowledgeBaseDocument document = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBase.getId(), filename, contentType, inputStream);

        assertThat(document).isNotNull();
        assertThat(document.getId()).isNotNull();
        assertThat(document.getName()).isEqualTo(filename);
        assertThat(document.getKnowledgeBaseId()).isEqualTo(knowledgeBase.getId());
        assertThat(document.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_UPLOADED);
        assertThat(document.getDocument()).isEqualTo(mockFileEntry);

        verify(knowledgeBaseFileStorage).storeDocument(eq(filename), any(InputStream.class));
    }

    @Test
    void testDeleteKnowledgeBaseDocument() {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName("Test Document");

        FileEntry documentFileEntry = new FileEntry("test.txt", "file://test/test.txt");

        document.setDocument(documentFileEntry);
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);

        document = knowledgeBaseDocumentRepository.save(document);

        KnowledgeBaseDocumentChunk chunk1 = new KnowledgeBaseDocumentChunk();

        chunk1.setKnowledgeBaseDocumentId(document.getId());
        chunk1.setVectorStoreId("vector-store-id-1");

        FileEntry chunkFileEntry1 = new FileEntry("chunk1.txt", "file://test/chunks/chunk1.txt");

        chunk1.setContent(chunkFileEntry1);

        knowledgeBaseDocumentChunkRepository.save(chunk1);

        KnowledgeBaseDocumentChunk chunk2 = new KnowledgeBaseDocumentChunk();

        chunk2.setKnowledgeBaseDocumentId(document.getId());
        chunk2.setVectorStoreId("vector-store-id-2");

        FileEntry chunkFileEntry2 = new FileEntry("chunk2.txt", "file://test/chunks/chunk2.txt");

        chunk2.setContent(chunkFileEntry2);

        knowledgeBaseDocumentChunkRepository.save(chunk2);

        Long documentId = document.getId();

        assertThat(knowledgeBaseDocumentRepository.findById(documentId)).isPresent();
        assertThat(knowledgeBaseDocumentChunkRepository.findAllByKnowledgeBaseDocumentId(documentId)).hasSize(2);

        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(documentId);

        assertThat(knowledgeBaseDocumentRepository.findById(documentId)).isNotPresent();
        assertThat(knowledgeBaseDocumentChunkRepository.findAllByKnowledgeBaseDocumentId(documentId)).isEmpty();

        verify(knowledgeBaseFileStorage).deleteDocument(documentFileEntry);
        verify(knowledgeBaseFileStorage).deleteChunkContent(chunkFileEntry1);
        verify(knowledgeBaseFileStorage).deleteChunkContent(chunkFileEntry2);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentWithoutChunks() {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName("Test Document");

        FileEntry documentFileEntry = new FileEntry("test.txt", "file://test/test.txt");

        document.setDocument(documentFileEntry);
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        document = knowledgeBaseDocumentRepository.save(document);

        Long documentId = document.getId();

        assertThat(knowledgeBaseDocumentRepository.findById(documentId)).isPresent();

        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(documentId);

        assertThat(knowledgeBaseDocumentRepository.findById(documentId)).isNotPresent();

        verify(knowledgeBaseFileStorage).deleteDocument(documentFileEntry);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentWithChunksWithoutContent() {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName("Test Document");

        FileEntry documentFileEntry = new FileEntry("test.txt", "file://test/test.txt");

        document.setDocument(documentFileEntry);
        document.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

        document = knowledgeBaseDocumentRepository.save(document);

        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("vector-store-id-1");

        knowledgeBaseDocumentChunkRepository.save(chunk);

        Long documentId = document.getId();

        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(documentId);

        assertThat(knowledgeBaseDocumentRepository.findById(documentId)).isNotPresent();
        assertThat(knowledgeBaseDocumentChunkRepository.findAllByKnowledgeBaseDocumentId(documentId)).isEmpty();
    }

    @Test
    void testCreateMultipleDocumentsForSameKnowledgeBase() {
        FileEntry mockFileEntry1 = new FileEntry("doc1.txt", "file://test/doc1.txt");
        FileEntry mockFileEntry2 = new FileEntry("doc2.txt", "file://test/doc2.txt");

        when(knowledgeBaseFileStorage.storeDocument(eq("doc1.txt"), any(InputStream.class))).thenReturn(mockFileEntry1);
        when(knowledgeBaseFileStorage.storeDocument(eq("doc2.txt"), any(InputStream.class))).thenReturn(mockFileEntry2);

        KnowledgeBaseDocument document1 = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBase.getId(), "doc1.txt", "text/plain",
            new ByteArrayInputStream("Content 1".getBytes(StandardCharsets.UTF_8)));

        KnowledgeBaseDocument document2 = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBase.getId(), "doc2.txt", "text/plain",
            new ByteArrayInputStream("Content 2".getBytes(StandardCharsets.UTF_8)));

        assertThat(document1.getId()).isNotEqualTo(document2.getId());
        assertThat(document1.getKnowledgeBaseId()).isEqualTo(knowledgeBase.getId());
        assertThat(document2.getKnowledgeBaseId()).isEqualTo(knowledgeBase.getId());

        List<KnowledgeBaseDocument> documents =
            knowledgeBaseDocumentRepository.findAllByKnowledgeBaseId(knowledgeBase.getId());

        assertThat(documents).hasSize(2);
    }
}
