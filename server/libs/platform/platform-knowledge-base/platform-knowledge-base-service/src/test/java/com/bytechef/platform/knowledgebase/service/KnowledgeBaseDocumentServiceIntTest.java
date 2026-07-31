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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.SmartApplicationListener;

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
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseFileStorage knowledgeBaseFileStorage;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseSourceRepository knowledgeBaseSourceRepository;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    public void beforeEach() {
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
    }

    @AfterEach
    public void afterEach() {
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseSourceRepository.deleteAll();
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

    @Test
    void testCreateSyncedDocumentPersistsAllSyncFieldsAndPublishesEvent() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        AtomicInteger eventCount = new AtomicInteger();

        applicationContext.addApplicationListener(new SmartApplicationListener() {

            @Override
            public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
                return PayloadApplicationEvent.class.isAssignableFrom(eventType);
            }

            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof PayloadApplicationEvent<?> payloadEvent
                    && payloadEvent.getPayload() instanceof KnowledgeBaseDocumentEvent) {

                    eventCount.incrementAndGet();
                }
            }
        });

        Instant now = Instant.parse("2026-05-08T12:00:00Z");

        KnowledgeBaseDocument created = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-1", "Record One", "Hello world",
            Map.of("kind", "contact"), null, "hash-1", now);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getKnowledgeBaseId()).isEqualTo(knowledgeBase.getId());
        assertThat(created.getSourceId()).isEqualTo(source.getId());
        assertThat(created.getSourceRecordId()).isEqualTo("rec-1");
        assertThat(created.getSyncedPayloadHash()).isEqualTo("hash-1");
        assertThat(created.getLastSeenAt()).isEqualTo(now);
        assertThat(created.getDeletedAt()).isNull();
        assertThat(created.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_UPLOADED);
        assertThat(created.getName()).isEqualTo("Record One");
        assertThat(created.getDocument()
            .getName()).isEqualTo("rec-1.md");
        assertThat(created.getTagNames()).contains("kind=contact");
        assertThat(eventCount.get()).isGreaterThan(0);

        verify(knowledgeBaseFileStorage, atLeastOnce()).storeDocument(anyString(), any(InputStream.class));
    }

    @Test
    void testReplaceSyncedDocumentClearsDeletedAtAndBumpsHash() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        Instant initial = Instant.parse("2026-05-08T10:00:00Z");
        KnowledgeBaseDocument original = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-1", "Record One", "Hello v1",
            Map.of("kind", "contact"), null, "hash-1", initial);

        // Mark the doc as tombstoned to verify replaceSyncedDocument clears deleted_at on re-appearance.
        original.setDeletedAt(Instant.parse("2026-05-08T11:00:00Z"));
        knowledgeBaseDocumentRepository.save(original);

        Instant later = Instant.parse("2026-05-08T12:00:00Z");

        KnowledgeBaseDocument replaced = impl.replaceSyncedDocument(
            original.getId(), "Record One v2", "Hello v2", Map.of("kind", "contact"), null, "hash-2", later);

        assertThat(replaced.getId()).isEqualTo(original.getId());
        assertThat(replaced.getSyncedPayloadHash()).isEqualTo("hash-2");
        assertThat(replaced.getLastSeenAt()).isEqualTo(later);
        assertThat(replaced.getDeletedAt()).isNull();
        assertThat(replaced.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_UPLOADED);
        assertThat(replaced.getName()).isEqualTo("Record One v2");
    }

    @Test
    void testReplaceSyncedDocumentTakesIdempotentFastPathWhenHashMatches() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        Instant initial = Instant.parse("2026-05-08T10:00:00Z");
        KnowledgeBaseDocument original = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-1", "Record One", "Hello v1",
            Map.of("kind", "contact"), null, "hash-1", initial);

        FileEntry originalFileEntry = original.getDocument();

        Instant later = Instant.parse("2026-05-08T12:00:00Z");

        KnowledgeBaseDocument replaced = impl.replaceSyncedDocument(
            original.getId(), "Record One", "Hello v1", Map.of("kind", "contact"), null, "hash-1", later);

        // Same hash + not tombstoned ⇒ fast path: only last_seen_at moves; name/document/hash unchanged.
        assertThat(replaced.getSyncedPayloadHash()).isEqualTo("hash-1");
        assertThat(replaced.getLastSeenAt()).isEqualTo(later);
        assertThat(replaced.getDocument()
            .getName())
                .isEqualTo(originalFileEntry.getName());
    }

    @Test
    void testTombstoneUnseenSetsDeletedAtForAbsentRecords() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        Instant initial = Instant.parse("2026-05-08T10:00:00Z");

        KnowledgeBaseDocument seenA = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-A", "Record A", "Body A", Map.of(), null, "hash-A", initial);
        KnowledgeBaseDocument seenB = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-B", "Record B", "Body B", Map.of(), null, "hash-B", initial);
        KnowledgeBaseDocument missingC = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-C", "Record C", "Body C", Map.of(), null, "hash-C", initial);

        Instant runEnd = Instant.parse("2026-05-08T12:00:00Z");

        int tombstoned = knowledgeBaseDocumentRepository.tombstoneUnseen(
            source.getId(), List.of("rec-A", "rec-B"), runEnd);

        assertThat(tombstoned).isEqualTo(1);
        assertThat(knowledgeBaseDocumentRepository.findById(seenA.getId())
            .orElseThrow()
            .getDeletedAt()).isNull();
        assertThat(knowledgeBaseDocumentRepository.findById(seenB.getId())
            .orElseThrow()
            .getDeletedAt()).isNull();
        assertThat(knowledgeBaseDocumentRepository.findById(missingC.getId())
            .orElseThrow()
            .getDeletedAt()).isEqualTo(runEnd);
    }

    @Test
    void testGetTombstonedDocumentsReturnsOnlyTombstonedRowsOfSource() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");
        KnowledgeBaseSource otherSource = persistSource("Airtable");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        Instant initial = Instant.parse("2026-05-08T10:00:00Z");

        impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-A", "Record A", "Body A", Map.of(), null, "hash-A", initial);
        KnowledgeBaseDocument tombstoned = impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-B", "Record B", "Body B", Map.of(), null, "hash-B", initial);
        KnowledgeBaseDocument otherSourceTombstoned = impl.createSyncedDocument(
            knowledgeBase.getId(), otherSource.getId(), "rec-C", "Record C", "Body C", Map.of(), null, "hash-C",
            initial);

        Instant runEnd = Instant.parse("2026-05-08T12:00:00Z");

        knowledgeBaseDocumentRepository.tombstoneUnseen(source.getId(), List.of("rec-A"), runEnd);
        knowledgeBaseDocumentRepository.tombstoneUnseen(otherSource.getId(), List.of("__never_matches__"), runEnd);

        List<KnowledgeBaseDocument> tombstonedDocuments = knowledgeBaseDocumentService.getTombstonedDocuments(
            source.getId());

        assertThat(tombstonedDocuments).extracting(KnowledgeBaseDocument::getId)
            .containsExactly(tombstoned.getId());

        List<KnowledgeBaseDocument> otherTombstonedDocuments = knowledgeBaseDocumentService.getTombstonedDocuments(
            otherSource.getId());

        assertThat(otherTombstonedDocuments).extracting(KnowledgeBaseDocument::getId)
            .containsExactly(otherSourceTombstoned.getId());
    }

    @Test
    void testTombstoneUnseenIgnoresManualUploads() {
        KnowledgeBaseDocumentServiceImpl impl = (KnowledgeBaseDocumentServiceImpl) knowledgeBaseDocumentService;
        KnowledgeBaseSource source = persistSource("HubSpot");

        when(knowledgeBaseFileStorage.storeDocument(anyString(), any(InputStream.class)))
            .thenAnswer(
                invocation -> new FileEntry(invocation.getArgument(0), "file://stored/" + invocation.getArgument(0)));

        Instant initial = Instant.parse("2026-05-08T10:00:00Z");

        impl.createSyncedDocument(
            knowledgeBase.getId(), source.getId(), "rec-A", "Record A", "Body A", Map.of(), null, "hash-A", initial);

        // Manual upload — source_id is NULL, so it must not be touched by the tombstone sweep.
        KnowledgeBaseDocument manualUpload = knowledgeBaseDocumentRepository.save(createDocument("Manual"));

        Instant runEnd = Instant.parse("2026-05-08T12:00:00Z");

        // Empty seen set ⇒ all source-tagged rows for this source are unseen.
        int tombstoned = knowledgeBaseDocumentRepository.tombstoneUnseen(
            source.getId(), List.of("__never_matches__"), runEnd);

        assertThat(tombstoned).isEqualTo(1);
        assertThat(knowledgeBaseDocumentRepository.findById(manualUpload.getId())
            .orElseThrow()
            .getDeletedAt()).isNull();
    }

    private KnowledgeBaseSource persistSource(String name) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setName(name);
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setKnowledgeBaseId(knowledgeBase.getId());
        source.setCadence("@hourly");
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        return knowledgeBaseSourceRepository.save(source);
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
