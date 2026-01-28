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
import com.bytechef.automation.knowledgebase.dto.DocumentStatusUpdate;
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
 * Integration tests for {@link KnowledgeBaseDocumentService}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseDocumentServiceIntTest {

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    public void beforeEach() {
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
    }

    @AfterEach
    public void afterEach() {
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testSaveKnowledgeBaseDocument() {
        KnowledgeBaseDocument document = createDocument("Test Document");

        KnowledgeBaseDocument savedDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document);

        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedDocument.getName()).isEqualTo("Test Document");
        assertThat(savedDocument.getKnowledgeBaseId()).isEqualTo(knowledgeBase.getId());
        assertThat(savedDocument.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_UPLOADED);
    }

    @Test
    void testGetKnowledgeBaseDocument() {
        KnowledgeBaseDocument document = knowledgeBaseDocumentRepository.save(createDocument("Test Document"));

        KnowledgeBaseDocument retrievedDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            document.getId());

        assertThat(retrievedDocument).isNotNull();
        assertThat(retrievedDocument.getId()).isEqualTo(document.getId());
        assertThat(retrievedDocument.getName()).isEqualTo("Test Document");
    }

    @Test
    void testGetKnowledgeBaseDocumentNotFound() {
        assertThatThrownBy(() -> knowledgeBaseDocumentService.getKnowledgeBaseDocument(Long.MAX_VALUE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("KnowledgeBase document not found");
    }

    @Test
    void testGetKnowledgeBaseDocuments() {
        knowledgeBaseDocumentRepository.save(createDocument("Document 1"));
        knowledgeBaseDocumentRepository.save(createDocument("Document 2"));
        knowledgeBaseDocumentRepository.save(createDocument("Document 3"));

        List<KnowledgeBaseDocument> documents =
            knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBase.getId());

        assertThat(documents).hasSize(3);
    }

    @Test
    void testGetKnowledgeBaseDocumentStatus() {
        KnowledgeBaseDocument document = createDocument("Test Document");

        document.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

        document = knowledgeBaseDocumentRepository.save(document);

        DocumentStatusUpdate statusUpdate =
            knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(document.getId());

        assertThat(statusUpdate).isNotNull();
        assertThat(statusUpdate.documentId()).isEqualTo(document.getId());
        assertThat(statusUpdate.status()).isEqualTo(KnowledgeBaseDocument.STATUS_PROCESSING);
    }

    @Test
    void testDeleteKnowledgeBaseDocument() {
        KnowledgeBaseDocument document = knowledgeBaseDocumentRepository.save(createDocument("Test Document"));

        assertThat(knowledgeBaseDocumentRepository.findById(document.getId())).isPresent();

        knowledgeBaseDocumentService.delete(document.getId());

        assertThat(knowledgeBaseDocumentRepository.findById(document.getId())).isNotPresent();
    }

    @Test
    void testUpdateDocumentStatus() {
        KnowledgeBaseDocument document = knowledgeBaseDocumentRepository.save(createDocument("Test Document"));

        assertThat(document.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_UPLOADED);

        document.setStatus(KnowledgeBaseDocument.STATUS_READY);

        KnowledgeBaseDocument updatedDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document);

        assertThat(updatedDocument.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_READY);
    }

    private KnowledgeBaseDocument createDocument(String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName(name);
        document.setDocument(new FileEntry(name + ".txt", "file://test/" + name + ".txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        return document;
    }
}
