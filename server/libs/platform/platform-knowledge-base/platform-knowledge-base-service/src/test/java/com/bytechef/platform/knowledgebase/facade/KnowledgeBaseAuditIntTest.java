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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditEvents;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseDocumentNotFoundException;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        KnowledgeBaseIntTestConfiguration.class, KnowledgeBaseAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String FILENAME = "handbook.txt";

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseFileStorage knowledgeBaseFileStorage;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void beforeEach() {
        deleteKnowledgeBases();

        KnowledgeBase newKnowledgeBase = new KnowledgeBase();

        newKnowledgeBase.setName("Support articles");

        knowledgeBase = knowledgeBaseRepository.save(newKnowledgeBase);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        deleteKnowledgeBases();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateKnowledgeBaseDocumentRecordsTheDocument() {
        when(knowledgeBaseFileStorage.storeDocument(eq(FILENAME), any(InputStream.class)))
            .thenReturn(new FileEntry(FILENAME, "file://test/" + FILENAME));

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBase.getId(), FILENAME, "text/plain", newInputStream());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DOCUMENT_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseDocumentId", String.valueOf(knowledgeBaseDocument.getId()))
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .containsEntry("filename", FILENAME)
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testCreateKnowledgeBaseDocumentWhoseStorageFailsRecordsAnError() {
        when(knowledgeBaseFileStorage.storeDocument(eq(FILENAME), any(InputStream.class)))
            .thenThrow(new IllegalStateException("storage unavailable"));

        Long knowledgeBaseId = knowledgeBase.getId();

        assertThatThrownBy(() -> knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBaseId, FILENAME, "text/plain", newInputStream()))
                .isInstanceOf(IllegalStateException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DOCUMENT_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName())
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBaseId))
            .containsEntry("filename", FILENAME)
            .doesNotContainKey("knowledgeBaseDocumentId");
        assertThat(knowledgeBaseDocumentRepository.count()).isZero();
    }

    @Test
    void testDeleteKnowledgeBaseDocumentRecordsTheDocument() {
        KnowledgeBaseDocument knowledgeBaseDocument = saveKnowledgeBaseDocument();

        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(knowledgeBaseDocument.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DOCUMENT_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseDocumentId", String.valueOf(knowledgeBaseDocument.getId()))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseDocumentRepository.findById(knowledgeBaseDocument.getId())).isEmpty();
    }

    @Test
    void testDeleteOfAnUnknownKnowledgeBaseDocumentRecordsAnError() {
        assertThatThrownBy(() -> knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(Long.MAX_VALUE))
            .isInstanceOf(KnowledgeBaseDocumentNotFoundException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DOCUMENT_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", KnowledgeBaseDocumentNotFoundException.class.getName())
            .containsEntry("knowledgeBaseDocumentId", String.valueOf(Long.MAX_VALUE));
    }

    @Test
    void testUpdateKnowledgeBaseRecordsTheNewName() {
        KnowledgeBase renamedKnowledgeBase = new KnowledgeBase();

        renamedKnowledgeBase.setName("Renamed articles");

        knowledgeBaseService.updateKnowledgeBase(knowledgeBase.getId(), renamedKnowledgeBase);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .containsEntry("name", "Renamed articles")
            .doesNotContainKeys("dataError", "errorClass", "mapperError");

        KnowledgeBase savedKnowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBase.getId());

        assertThat(savedKnowledgeBase.getName()).isEqualTo("Renamed articles");
    }

    @Test
    void testUpdateOfAnUnknownKnowledgeBaseRecordsAnError() {
        KnowledgeBase renamedKnowledgeBase = new KnowledgeBase();

        renamedKnowledgeBase.setName("Renamed articles");

        assertThatThrownBy(() -> knowledgeBaseService.updateKnowledgeBase(Long.MAX_VALUE, renamedKnowledgeBase))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("knowledgeBaseId", String.valueOf(Long.MAX_VALUE))
            .doesNotContainKey("name");
    }

    private void deleteKnowledgeBases() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private static InputStream newInputStream() {
        return new ByteArrayInputStream("Reset your password from the login page".getBytes(StandardCharsets.UTF_8));
    }

    private KnowledgeBaseDocument saveKnowledgeBaseDocument() {
        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setDocument(new FileEntry(FILENAME, "file://test/" + FILENAME));
        knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBase.getId());
        knowledgeBaseDocument.setName(FILENAME);
        knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_READY);

        return knowledgeBaseDocumentRepository.save(knowledgeBaseDocument);
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }
    }
}
