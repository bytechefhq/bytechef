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

package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class KnowledgeBaseFacadeIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseFacade knowledgeBaseFacade;

    @MockitoBean(name = "knowledgeBasePgVectorStore")
    private VectorStore vectorStore;

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
        EnvironmentContext.clear();

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

    @Test
    void testSetsEnvironmentDuringSearchAndClearsAfter() {
        KnowledgeBase stagingKnowledgeBase = new KnowledgeBase();

        stagingKnowledgeBase.setEnvironment(Environment.STAGING);
        stagingKnowledgeBase.setName("Staging KnowledgeBase");

        stagingKnowledgeBase = knowledgeBaseRepository.save(stagingKnowledgeBase);

        AtomicReference<Environment> observedEnvironment = new AtomicReference<>();

        doAnswer(invocation -> {
            observedEnvironment.set(EnvironmentContext.getCurrentEnvironment());

            return List.of();
        }).when(vectorStore)
            .similaritySearch(any(SearchRequest.class));

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseFacade.searchKnowledgeBase(stagingKnowledgeBase.getId(), "query", null);

        assertThat(chunks).isEmpty();
        assertThat(observedEnvironment.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
