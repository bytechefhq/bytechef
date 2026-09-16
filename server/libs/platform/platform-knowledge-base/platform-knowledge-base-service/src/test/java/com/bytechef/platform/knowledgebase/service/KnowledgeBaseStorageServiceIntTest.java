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

package com.bytechef.platform.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseStorageServiceIntTest {

    private static final long DEFAULT_MAX_SIZE_BYTES = new ApplicationProperties.Ai.KnowledgeBase().getMaxSizeBytes();
    private static final String TRY_LOCK_SQL =
        "SELECT pg_try_advisory_xact_lock(hashtext('knowledge_base_storage'), hashtext(current_schema()))";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseStorageService knowledgeBaseStorageService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void beforeEach() {
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

    }

    @AfterEach
    void afterEach() {
        getKnowledgeBaseProperties().setMaxSizeBytes(DEFAULT_MAX_SIZE_BYTES);

        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testGetUsageSumsStoredDocumentSizes() {
        setLimit(1_000L);

        saveDocument("a.txt", 300L);
        saveDocument("b.txt", 500L);
        saveDocument("c.txt", null);

        KnowledgeBaseStorageUsage usage = knowledgeBaseStorageService.getUsage();

        assertThat(usage.usedBytes()).isEqualTo(800L);
        assertThat(usage.limitBytes()).isEqualTo(1_000L);
        assertThat(usage.percentage()).isEqualTo(80.0);
        assertThat(usage.unlimited()).isFalse();
    }

    @Test
    void testGetUsageReportsUnlimitedWhenLimitIsZero() {
        setLimit(0L);

        saveDocument("a.txt", 300L);

        KnowledgeBaseStorageUsage usage = knowledgeBaseStorageService.getUsage();

        assertThat(usage.usedBytes()).isEqualTo(300L);
        assertThat(usage.unlimited()).isTrue();
        assertThat(usage.percentage()).isEqualTo(0.0);
    }

    @Test
    void testCheckLimitThrowsWhenIncomingExceeds() {
        setLimit(1_000L);

        saveDocument("a.txt", 900L);

        assertThatThrownBy(() -> checkLimitInTransaction(200))
            .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);
    }

    @Test
    void testCheckLimitPassesAtExactBoundary() {
        setLimit(1_000L);

        saveDocument("a.txt", 900L);

        assertThatCode(() -> checkLimitInTransaction(100)).doesNotThrowAnyException();
    }

    @Test
    void testCheckLimitPassesWhenUnlimited() {
        setLimit(0L);

        saveDocument("a.txt", 999_999L);

        assertThatCode(() -> checkLimitInTransaction(1_000_000)).doesNotThrowAnyException();
    }

    @Test
    void testCheckLimitMessageKeepsSmallSizesReadable() {
        setLimit(2_048L);

        saveDocument("a.txt", 900L);

        assertThatThrownBy(() -> checkLimitInTransaction(2_000))
            .hasMessageContaining("900 B of 2 KB");
    }

    @Test
    void testCheckLimitRequiresATransaction() {
        setLimit(1_000L);

        assertThatThrownBy(() -> knowledgeBaseStorageService.checkLimit(100))
            .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void testCheckLimitHoldsTheTenantLockUntilCommit() {
        setLimit(1_000L);

        AtomicReference<Boolean> lockFreeWhileHeld = new AtomicReference<>();

        transactionTemplate.executeWithoutResult(status -> {
            knowledgeBaseStorageService.checkLimit(100);

            Thread otherWriter = Thread.ofVirtual()
                .start(() -> lockFreeWhileHeld.set(jdbcTemplate.queryForObject(TRY_LOCK_SQL, Boolean.class)));

            try {
                otherWriter.join();
            } catch (InterruptedException exception) {
                Thread.currentThread()
                    .interrupt();
            }
        });

        assertThat(lockFreeWhileHeld.get()).isFalse();
        assertThat(jdbcTemplate.queryForObject(TRY_LOCK_SQL, Boolean.class)).isTrue();
    }

    private void checkLimitInTransaction(long incomingBytes) {
        transactionTemplate.executeWithoutResult(status -> knowledgeBaseStorageService.checkLimit(incomingBytes));
    }

    private ApplicationProperties.Ai.KnowledgeBase getKnowledgeBaseProperties() {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return ai.getKnowledgeBase();
    }

    private void saveDocument(String name, Long documentSize) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setName(name);
        document.setDocument(new FileEntry(name, "file://test/" + name));
        document.setDocumentSize(documentSize);
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        knowledgeBaseDocumentRepository.save(document);
    }

    private void setLimit(long maxSizeBytes) {
        getKnowledgeBaseProperties().setMaxSizeBytes(maxSizeBytes);
    }
}
