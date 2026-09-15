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

package com.bytechef.platform.knowledgebase.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.web.rest.config.PlatformKnowledgeBaseRestTestConfiguration;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link KnowledgeBaseDocumentApiController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformKnowledgeBaseRestTestConfiguration.class)
@WebMvcTest(
    value = KnowledgeBaseDocumentApiController.class,
    properties = "bytechef.ai.knowledge-base.enabled=true")
class KnowledgeBaseDocumentApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Test
    void testUploadDocument() throws Exception {
        Long knowledgeBaseId = 1L;
        String filename = "test-document.txt";
        String contentType = "text/plain";
        String content = "Test document content";

        KnowledgeBaseDocument mockDocument = createMockDocument(1L, filename);

        when(knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            eq(knowledgeBaseId), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(mockDocument);

        MockMultipartFile file = new MockMultipartFile(
            "file", filename, contentType, content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/internal/knowledge-bases/{id}/documents", knowledgeBaseId).file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value(filename));

        verify(knowledgeBaseDocumentFacade).createKnowledgeBaseDocument(
            eq(knowledgeBaseId), eq(filename), eq(contentType), any(InputStream.class));
    }

    @Test
    void testUploadDocumentWithDifferentContentType() throws Exception {
        Long knowledgeBaseId = 1L;
        String filename = "document.pdf";
        String contentType = "application/pdf";
        String content = "PDF content";

        KnowledgeBaseDocument mockDocument = createMockDocument(1L, filename);

        when(knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            eq(knowledgeBaseId), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(mockDocument);

        MockMultipartFile file = new MockMultipartFile(
            "file", filename, contentType, content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/internal/knowledge-bases/{id}/documents", knowledgeBaseId).file(file))
            .andExpect(status().isOk());

        verify(knowledgeBaseDocumentFacade).createKnowledgeBaseDocument(
            eq(knowledgeBaseId), eq(filename), eq(contentType), any(InputStream.class));
    }

    @Test
    void testUploadDocumentToMultipleKnowledgeBases() throws Exception {
        String filename = "shared-document.txt";
        String contentType = "text/plain";
        String content = "Shared document content";

        KnowledgeBaseDocument mockDocument1 = createMockDocument(1L, filename);
        KnowledgeBaseDocument mockDocument2 = createMockDocument(2L, filename);

        when(knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            eq(1L), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(mockDocument1);
        when(knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            eq(2L), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(mockDocument2);

        MockMultipartFile file1 = new MockMultipartFile(
            "file", filename, contentType, content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/internal/knowledge-bases/{id}/documents", 1L).file(file1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        MockMultipartFile file2 = new MockMultipartFile(
            "file", filename, contentType, content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/internal/knowledge-bases/{id}/documents", 2L).file(file2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2));
    }

    private KnowledgeBaseDocument createMockDocument(Long id, String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setKnowledgeBaseId(1L);
        document.setName(name);
        document.setDocument(new FileEntry(name, "file://test/" + name));
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);
        document.setVersion(1);

        return document;
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long KNOWLEDGE_BASE_ID = 11L;

        @Autowired
        private KnowledgeBaseDocumentApiController knowledgeBaseDocumentApiController;

        @Autowired
        private PermissionService permissionService;

        private final MockMultipartFile file = new MockMultipartFile(
            "file", "document.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

        @BeforeEach
        void beforeEach() {
            when(knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(anyLong(), any(), any(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testUploadDocumentDeniesWhenTheKnowledgeBaseEditScopeIsRefused() {
            assertThatThrownBy(() -> knowledgeBaseDocumentApiController.uploadDocument(KNOWLEDGE_BASE_ID, file))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", "KNOWLEDGE_BASE_EDIT");
        }

        @Test
        void testUploadDocumentAllowsWhenTheKnowledgeBaseEditScopeIsGranted() {
            when(permissionService.hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", "KNOWLEDGE_BASE_EDIT"))
                .thenReturn(true);

            assertThatThrownBy(() -> knowledgeBaseDocumentApiController.uploadDocument(KNOWLEDGE_BASE_ID, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames =
                Arrays.stream(KnowledgeBaseDocumentApiController.class.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            Set<String> coveredMethodNames = Set.of("uploadDocument");

            assertThat(coveredMethodNames).isEqualTo(guardedMethodNames);
        }

        @EnableMethodSecurity
        @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
        static class MethodSecurityConfiguration {

            @Bean
            PermissionService permissionService() {
                return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
            }
        }
    }
}
