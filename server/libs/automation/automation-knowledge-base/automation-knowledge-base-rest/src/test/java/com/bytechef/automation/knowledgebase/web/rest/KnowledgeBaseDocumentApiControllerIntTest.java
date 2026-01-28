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

package com.bytechef.automation.knowledgebase.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.web.rest.config.AutomationKnowledgeBaseRestTestConfiguration;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link KnowledgeBaseDocumentApiController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationKnowledgeBaseRestTestConfiguration.class)
@WebMvcTest(value = KnowledgeBaseDocumentApiController.class)
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
}
