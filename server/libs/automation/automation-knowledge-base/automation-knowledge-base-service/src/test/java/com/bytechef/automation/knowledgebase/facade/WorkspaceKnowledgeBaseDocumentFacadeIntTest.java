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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@SpringJUnitConfig(WorkspaceKnowledgeBaseDocumentFacadeIntTest.MethodSecurityConfiguration.class)
class WorkspaceKnowledgeBaseDocumentFacadeIntTest {

    private static final long KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID = 1303;
    private static final long KNOWLEDGE_BASE_DOCUMENT_ID = 1302;
    private static final long KNOWLEDGE_BASE_ID = 1301;
    private static final String KNOWLEDGE_BASE_EDIT = "KNOWLEDGE_BASE_EDIT";
    private static final String KNOWLEDGE_BASE_VIEW = "KNOWLEDGE_BASE_VIEW";

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceKnowledgeBaseDocumentFacade workspaceKnowledgeBaseDocumentFacade;

    @BeforeEach
    void beforeEach() {
        reset(
            knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentFacade, knowledgeBaseDocumentService,
            knowledgeBaseDocumentTagService, permissionService);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "n/a", List.of()));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteKnowledgeBaseDocumentRequiresEditScopeOnTheDocument() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(KNOWLEDGE_BASE_DOCUMENT_ID),
            KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

        verify(knowledgeBaseDocumentFacade).deleteKnowledgeBaseDocument(KNOWLEDGE_BASE_DOCUMENT_ID);
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunkRequiresEditScopeOnTheChunk() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.deleteKnowledgeBaseDocumentChunk(
                KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID),
            KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, "KnowledgeBaseDocumentChunk", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

        verify(knowledgeBaseDocumentChunkFacade).deleteKnowledgeBaseDocumentChunk(KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID);
    }

    @Test
    void testGetKnowledgeBaseDocumentRequiresViewScopeOnTheDocument() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.getKnowledgeBaseDocument(KNOWLEDGE_BASE_DOCUMENT_ID),
            KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", KNOWLEDGE_BASE_VIEW, null);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocument(KNOWLEDGE_BASE_DOCUMENT_ID);
    }

    @Test
    void testGetKnowledgeBaseDocumentChunksRequiresViewScopeOnTheDocument() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.getKnowledgeBaseDocumentChunks(KNOWLEDGE_BASE_DOCUMENT_ID),
            KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", KNOWLEDGE_BASE_VIEW, null);

        verify(knowledgeBaseDocumentChunkFacade).getKnowledgeBaseDocumentChunksByDocumentId(KNOWLEDGE_BASE_DOCUMENT_ID);
    }

    @Test
    void testGetKnowledgeBaseDocumentStatusRequiresViewScopeOnTheDocument() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.getKnowledgeBaseDocumentStatus(KNOWLEDGE_BASE_DOCUMENT_ID),
            KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", KNOWLEDGE_BASE_VIEW, null);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocumentStatus(KNOWLEDGE_BASE_DOCUMENT_ID);
    }

    @Test
    void testGetKnowledgeBaseDocumentTagsRequiresViewScopeOnTheKnowledgeBase() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.getKnowledgeBaseDocumentTags(KNOWLEDGE_BASE_ID),
            KNOWLEDGE_BASE_ID, "KnowledgeBase", KNOWLEDGE_BASE_VIEW, null);

        verify(knowledgeBaseDocumentTagService).getTagNamesByKnowledgeBaseId(KNOWLEDGE_BASE_ID);
    }

    @Test
    void testGetKnowledgeBaseDocumentTagsByDocumentIdRequiresViewScopeOnTheKnowledgeBase() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.getKnowledgeBaseDocumentTagsByDocumentId(KNOWLEDGE_BASE_ID),
            KNOWLEDGE_BASE_ID, "KnowledgeBase", KNOWLEDGE_BASE_VIEW, null);

        verify(knowledgeBaseDocumentTagService).getTagNamesByKnowledgeBaseDocumentId(KNOWLEDGE_BASE_ID);
    }

    @Test
    void testUpdateKnowledgeBaseDocumentChunkRequiresEditScopeOnTheChunk() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentChunk(
                KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, "Refunds take five days."),
            KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, "KnowledgeBaseDocumentChunk", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

        verify(knowledgeBaseDocumentChunkFacade).updateKnowledgeBaseDocumentChunk(
            KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, "Refunds take five days.");
    }

    @Test
    void testUpdateKnowledgeBaseDocumentTagsRequiresEditScopeOnTheDocument() {
        assertRequiresScope(
            () -> workspaceKnowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentTags(
                KNOWLEDGE_BASE_DOCUMENT_ID, List.of("billing")),
            KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", KNOWLEDGE_BASE_EDIT, KNOWLEDGE_BASE_VIEW);

        verify(knowledgeBaseDocumentFacade).updateKnowledgeBaseDocumentTags(
            KNOWLEDGE_BASE_DOCUMENT_ID, List.of("billing"));
    }

    private void assertRequiresScope(
        ThrowingCallable throwingCallable, long resourceId, String resourceType, String scope,
        @Nullable String lesserScope) {

        assertThatThrownBy(throwingCallable).isInstanceOf(AccessDeniedException.class);

        if (lesserScope != null) {
            when(permissionService.hasResourceScope(resourceId, resourceType, lesserScope)).thenReturn(true);

            assertThatThrownBy(throwingCallable).isInstanceOf(AccessDeniedException.class);

            reset(permissionService);
        }

        when(permissionService.hasResourceScope(resourceId + 1, resourceType, scope)).thenReturn(true);

        assertThatThrownBy(throwingCallable).isInstanceOf(AccessDeniedException.class);

        reset(permissionService);

        when(permissionService.hasResourceScope(resourceId, resourceType, scope)).thenReturn(true);

        assertThatCode(throwingCallable).doesNotThrowAnyException();
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade() {
            return mock(KnowledgeBaseDocumentChunkFacade.class);
        }

        @Bean
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade() {
            return mock(KnowledgeBaseDocumentFacade.class);
        }

        @Bean
        KnowledgeBaseDocumentService knowledgeBaseDocumentService() {
            return mock(KnowledgeBaseDocumentService.class);
        }

        @Bean
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService() {
            return mock(KnowledgeBaseDocumentTagService.class);
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        WorkspaceKnowledgeBaseDocumentFacade workspaceKnowledgeBaseDocumentFacade(
            KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
            KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
            KnowledgeBaseDocumentService knowledgeBaseDocumentService,
            KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

            return new WorkspaceKnowledgeBaseDocumentFacadeImpl(
                knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentFacade, knowledgeBaseDocumentService,
                knowledgeBaseDocumentTagService);
        }
    }
}
