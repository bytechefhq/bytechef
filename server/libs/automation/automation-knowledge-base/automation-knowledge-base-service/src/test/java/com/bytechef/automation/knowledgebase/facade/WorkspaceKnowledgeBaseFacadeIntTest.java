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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.config.AutomationKnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditEvents;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationKnowledgeBaseIntTestConfiguration.class,
        WorkspaceKnowledgeBaseFacadeIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceKnowledgeBaseFacadeIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long DEVELOPMENT_ENVIRONMENT_ID = 0;
    private static final long STAGING_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 9201;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceKnowledgeBaseDocumentFacade workspaceKnowledgeBaseDocumentFacade;

    @Autowired
    private WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @Autowired
    private WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @BeforeEach
    void beforeEach() {
        deleteKnowledgeBases();

        jdbcTemplate.update(
            "INSERT INTO workspace " +
                "(id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            WORKSPACE_ID, "knowledge-base-audit-workspace");

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        deleteKnowledgeBases();

        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheNewKnowledgeBaseAndWorkspace() {
        KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .containsEntry("name", "Support articles")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(DEVELOPMENT_ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isPresent();
    }

    @Test
    void testCreateWithAnInvalidEnvironmentRecordsAnError() {
        KnowledgeBase knowledgeBase = newKnowledgeBase("Support articles");

        assertThatThrownBy(
            () -> workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(knowledgeBase, WORKSPACE_ID, -1))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", "-1")
            .doesNotContainKeys("knowledgeBaseId", "name");
    }

    @Test
    void testCloneRecordsTheCloneAndItsSource() {
        KnowledgeBase sourceKnowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        KnowledgeBase clonedKnowledgeBase = workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
            sourceKnowledgeBase.getId(), WORKSPACE_ID, STAGING_ENVIRONMENT_ID, "Staging articles");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CLONED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(clonedKnowledgeBase.getId()))
            .containsEntry("name", "Staging articles")
            .containsEntry("sourceKnowledgeBaseId", String.valueOf(sourceKnowledgeBase.getId()))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(STAGING_ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(clonedKnowledgeBase.getId()).isNotEqualTo(sourceKnowledgeBase.getId());
    }

    @Test
    void testCloneOfAKnowledgeBaseOutsideTheWorkspaceRecordsAnError() {
        KnowledgeBase foreignKnowledgeBase = knowledgeBaseRepository.save(newKnowledgeBase("Foreign articles"));

        Long foreignKnowledgeBaseId = foreignKnowledgeBase.getId();

        assertThatThrownBy(() -> workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
            foreignKnowledgeBaseId, WORKSPACE_ID, STAGING_ENVIRONMENT_ID, null))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CLONED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("sourceKnowledgeBaseId", String.valueOf(foreignKnowledgeBaseId))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(STAGING_ENVIRONMENT_ID))
            .doesNotContainKeys("knowledgeBaseId", "name");
        assertThat(knowledgeBaseRepository.count()).isEqualTo(1);
    }

    @Test
    void testDeleteRecordsTheDeletedKnowledgeBase() {
        KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(knowledgeBase.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isEmpty();
    }

    @Test
    void testDeleteOfAnUnknownKnowledgeBaseRecordsAnError() {
        assertThatThrownBy(() -> workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("knowledgeBaseId", String.valueOf(Long.MAX_VALUE));
    }

    @Test
    void testUpdateRecordsTheNewName() {
        KnowledgeBase knowledgeBase = createKnowledgeBase();

        workspaceKnowledgeBaseFacade.updateKnowledgeBase(knowledgeBase.getId(), newKnowledgeBase("Renamed articles"));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .containsEntry("name", "Renamed articles")
            .doesNotContainKeys("dataError", "errorClass", "mapperError");

        KnowledgeBase savedKnowledgeBase = knowledgeBaseRepository.findById(knowledgeBase.getId())
            .orElseThrow();

        assertThat(savedKnowledgeBase.getName()).isEqualTo("Renamed articles");
    }

    @Test
    void testUpdateOfAnUnknownKnowledgeBaseRecordsAnError() {
        KnowledgeBase renamedKnowledgeBase = newKnowledgeBase("Renamed articles");

        assertThatThrownBy(
            () -> workspaceKnowledgeBaseFacade.updateKnowledgeBase(Long.MAX_VALUE, renamedKnowledgeBase))
                .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("knowledgeBaseId", String.valueOf(Long.MAX_VALUE))
            .doesNotContainKey("name");
    }

    @Test
    void testUpdateChunkRecordsTheChunkAndItsDocumentWithoutTheContent() {
        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = createChunk();

        workspaceKnowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentChunk(
            knowledgeBaseDocumentChunk.getId(), "confidential paragraph");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            KnowledgeBaseAuditEvents.KB_DOCUMENT_CHUNK_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseDocumentChunkId", String.valueOf(knowledgeBaseDocumentChunk.getId()))
            .containsEntry(
                "knowledgeBaseDocumentId", String.valueOf(knowledgeBaseDocumentChunk.getKnowledgeBaseDocumentId()))
            .doesNotContainValue("confidential paragraph")
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testUpdateOfAnUnknownChunkRecordsAnError() {
        assertThatThrownBy(
            () -> workspaceKnowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentChunk(Long.MAX_VALUE, "paragraph"))
                .isExactlyInstanceOf(RuntimeException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            KnowledgeBaseAuditEvents.KB_DOCUMENT_CHUNK_UPDATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", RuntimeException.class.getName())
            .containsEntry("knowledgeBaseDocumentChunkId", String.valueOf(Long.MAX_VALUE))
            .doesNotContainKey("knowledgeBaseDocumentId");
    }

    @Test
    void testDeleteChunkRecordsTheChunk() {
        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = createChunk();

        workspaceKnowledgeBaseDocumentFacade.deleteKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunk.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            KnowledgeBaseAuditEvents.KB_DOCUMENT_CHUNK_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseDocumentChunkId", String.valueOf(knowledgeBaseDocumentChunk.getId()))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseDocumentChunkRepository.findById(knowledgeBaseDocumentChunk.getId())).isEmpty();
    }

    @Test
    void testDeleteOfAnUnknownChunkRecordsAnError() {
        assertThatThrownBy(
            () -> workspaceKnowledgeBaseDocumentFacade.deleteKnowledgeBaseDocumentChunk(Long.MAX_VALUE))
                .isExactlyInstanceOf(RuntimeException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            KnowledgeBaseAuditEvents.KB_DOCUMENT_CHUNK_DELETED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", RuntimeException.class.getName())
            .containsEntry("knowledgeBaseDocumentChunkId", String.valueOf(Long.MAX_VALUE));
    }

    private KnowledgeBaseDocumentChunk createChunk() {
        KnowledgeBase knowledgeBase = createKnowledgeBase();

        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBase.getId());
        knowledgeBaseDocument.setName("handbook.txt");
        knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_READY);

        knowledgeBaseDocument = knowledgeBaseDocumentRepository.save(knowledgeBaseDocument);

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

        knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(knowledgeBaseDocument.getId());
        knowledgeBaseDocumentChunk.setTextContent("first paragraph");

        return knowledgeBaseDocumentChunkRepository.save(knowledgeBaseDocumentChunk);
    }

    private KnowledgeBase createKnowledgeBase() {
        KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        return knowledgeBase;
    }

    private void deleteKnowledgeBases() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        workspaceKnowledgeBaseRepository.deleteAll();
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

    private static KnowledgeBase newKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);

        return knowledgeBase;
    }

    @Nested
    @ContextConfiguration(classes = PreAuthorizeEnforcement.MethodSecurityConfiguration.class)
    class PreAuthorizeEnforcement {

        private static final String KNOWLEDGE_BASE_CREATE = "KNOWLEDGE_BASE_CREATE";
        private static final String KNOWLEDGE_BASE_EDIT = "KNOWLEDGE_BASE_EDIT";
        private static final String KNOWLEDGE_BASE_VIEW = "KNOWLEDGE_BASE_VIEW";

        @Autowired
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            reset(permissionService);
        }

        @Test
        void testCreateWorkspaceKnowledgeBaseRequiresCreateScopeOnTheWorkspace() {
            assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
                    newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID),
                WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_CREATE, KNOWLEDGE_BASE_VIEW);

            assertThat(knowledgeBaseRepository.count()).isEqualTo(1);
        }

        @Test
        void testCloneWorkspaceKnowledgeBaseRequiresCreateScopeOnTheWorkspace() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
                    knowledgeBase.getId(), WORKSPACE_ID, STAGING_ENVIRONMENT_ID, "Staging articles"),
                WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_CREATE, KNOWLEDGE_BASE_VIEW);

            assertThat(knowledgeBaseRepository.count()).isEqualTo(2);
        }

        @Test
        void testGetWorkspaceKnowledgeBasesRequiresViewScopeOnTheWorkspace() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            List<KnowledgeBase> knowledgeBases = assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(
                    WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID),
                WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_VIEW, null);

            assertThat(knowledgeBases)
                .extracting(KnowledgeBase::getId)
                .containsExactly(knowledgeBase.getId());
        }

        @Test
        void testGetWorkspaceKnowledgeBaseTagsRequiresViewScopeOnTheWorkspace() {
            assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBaseTags(WORKSPACE_ID),
                WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_VIEW, null);
        }

        @Test
        void testGetWorkspaceKnowledgeBaseTagsByKnowledgeBaseIdRequiresViewScopeOnTheWorkspace() {
            assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBaseTagsByKnowledgeBaseId(WORKSPACE_ID),
                WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_VIEW, null);
        }

        @Test
        void testGetKnowledgeBaseRequiresViewScopeOnTheKnowledgeBase() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            KnowledgeBase fetchedKnowledgeBase = assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.getKnowledgeBase(knowledgeBase.getId()),
                knowledgeBase.getId(), "KnowledgeBase", KNOWLEDGE_BASE_VIEW, null);

            assertThat(fetchedKnowledgeBase.getName()).isEqualTo("Support articles");
        }

        @Test
        void testSearchKnowledgeBaseRequiresViewScopeOnTheKnowledgeBase() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.searchKnowledgeBase(knowledgeBase.getId(), "refunds", null),
                knowledgeBase.getId(), "KnowledgeBase", KNOWLEDGE_BASE_VIEW, null);
        }

        @Test
        void testUpdateKnowledgeBaseRequiresEditScopeOnTheKnowledgeBase() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            KnowledgeBase updatedKnowledgeBase = assertRequiresScope(
                () -> workspaceKnowledgeBaseFacade.updateKnowledgeBase(
                    knowledgeBase.getId(), newKnowledgeBase("Renamed articles")),
                knowledgeBase.getId(), "KnowledgeBase", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

            assertThat(updatedKnowledgeBase.getName()).isEqualTo("Renamed articles");
        }

        @Test
        void testUpdateKnowledgeBaseTagsRequiresEditScopeOnTheKnowledgeBase() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            assertRequiresScope(
                () -> {
                    workspaceKnowledgeBaseFacade.updateKnowledgeBaseTags(knowledgeBase.getId(), List.of());

                    return null;
                },
                knowledgeBase.getId(), "KnowledgeBase", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);
        }

        @Test
        void testDeleteWorkspaceKnowledgeBaseRequiresEditScopeOnTheKnowledgeBase() {
            KnowledgeBase knowledgeBase = createKnowledgeBase("Support articles");

            assertRequiresScope(
                () -> {
                    workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(knowledgeBase.getId());

                    return null;
                },
                knowledgeBase.getId(), "KnowledgeBase", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

            assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isEmpty();
        }

        @Test
        void testGetStorageUsageNeedsNoResourceScope() {
            assertThat(workspaceKnowledgeBaseFacade.getStorageUsage()).isNotNull();

            verifyNoInteractions(permissionService);
        }

        private <T> T assertRequiresScope(
            Callable<T> callable, long resourceId, String resourceType, String scope, @Nullable String lesserScope) {

            assertThatThrownBy(callable::call).isInstanceOf(AccessDeniedException.class);

            if (lesserScope != null) {
                when(permissionService.hasResourceScope(resourceId, resourceType, lesserScope)).thenReturn(true);

                assertThatThrownBy(callable::call).isInstanceOf(AccessDeniedException.class);

                reset(permissionService);
            }

            when(permissionService.hasResourceScope(resourceId + 1, resourceType, scope)).thenReturn(true);

            assertThatThrownBy(callable::call).isInstanceOf(AccessDeniedException.class);

            reset(permissionService);

            when(permissionService.hasResourceScope(resourceId, resourceType, scope)).thenReturn(true);

            try {
                return callable.call();
            } catch (Exception exception) {
                throw new AssertionError("Expected the granted call to succeed", exception);
            }
        }

        private KnowledgeBase createKnowledgeBase(String name) {
            when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", KNOWLEDGE_BASE_CREATE))
                .thenReturn(true);

            KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
                newKnowledgeBase(name), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

            reset(permissionService);

            return knowledgeBase;
        }

        @EnableMethodSecurity
        @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
        static class MethodSecurityConfiguration {

            @Bean("permissionService")
            PermissionService permissionService() {
                return mock(PermissionService.class);
            }
        }
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
