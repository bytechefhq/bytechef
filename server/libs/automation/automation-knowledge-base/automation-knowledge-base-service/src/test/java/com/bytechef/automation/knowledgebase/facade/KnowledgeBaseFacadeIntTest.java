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

import com.bytechef.automation.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class KnowledgeBaseFacadeIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

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
    void testUpdateKnowledgeBaseDocumentChunk() {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(document.getId());
        chunk.setVectorStoreId("test-vector-store-id");

        chunk = knowledgeBaseDocumentChunkRepository.save(chunk);

        KnowledgeBaseDocumentChunk updatedChunk =
            knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(chunk.getId(), "Updated content");

        assertThat(updatedChunk.getTextContent()).isEqualTo("Updated content");
    }
}
