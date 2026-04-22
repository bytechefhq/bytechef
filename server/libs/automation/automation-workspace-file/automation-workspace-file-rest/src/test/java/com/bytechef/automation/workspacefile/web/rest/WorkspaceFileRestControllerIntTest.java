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

package com.bytechef.automation.workspacefile.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.domain.WorkspaceFileSource;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import com.bytechef.automation.workspacefile.web.rest.config.AutomationWorkspaceFileRestTestConfiguration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link WorkspaceFileRestController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationWorkspaceFileRestTestConfiguration.class)
@WebMvcTest(value = WorkspaceFileRestController.class)
class WorkspaceFileRestControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceFileFacade workspaceFileFacade;

    @Test
    void testUploadReturns201AndDto() throws Exception {
        Long workspaceId = 1L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] content = "# Heading".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile created = createWorkspaceFile(42L, filename, contentType, content.length);

        when(workspaceFileFacade.createFromUpload(
            eq(workspaceId), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(created);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        mockMvc
            .perform(multipart("/api/automation/internal/workspace-files/upload")
                .file(file)
                .param("workspaceId", workspaceId.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.name").value(filename))
            .andExpect(jsonPath("$.mimeType").value(contentType))
            .andExpect(jsonPath("$.source").value(WorkspaceFileSource.USER_UPLOAD.name()));

        verify(workspaceFileFacade).createFromUpload(
            eq(workspaceId), eq(filename), eq(contentType), any(InputStream.class));
    }

    @Test
    void testDownloadReturnsContent() throws Exception {
        Long id = 42L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] contentBytes = "# Downloaded".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile workspaceFile = createWorkspaceFile(id, filename, contentType, contentBytes.length);

        when(workspaceFileFacade.findById(id)).thenReturn(workspaceFile);
        when(workspaceFileFacade.downloadContent(id)).thenReturn(new ByteArrayInputStream(contentBytes));

        MvcResult asyncResult = mockMvc
            .perform(get("/api/automation/internal/workspace-files/{id}/content", id))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc
            .perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(content().bytes(contentBytes));
    }

    @Test
    void testPutContentUpdates() throws Exception {
        Long id = 42L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] updatedBytes = "# Updated".getBytes(StandardCharsets.UTF_8);

        WorkspaceFile updated = createWorkspaceFile(id, filename, contentType, updatedBytes.length);

        when(workspaceFileFacade.updateContent(eq(id), eq(contentType), any(InputStream.class))).thenReturn(updated);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, updatedBytes);

        mockMvc
            .perform(multipart("/api/automation/internal/workspace-files/{id}/content", id)
                .file(file)
                .with(multipartRequest -> {
                    multipartRequest.setMethod("PUT");

                    return multipartRequest;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.sizeBytes").value(updatedBytes.length));

        verify(workspaceFileFacade).updateContent(eq(id), eq(contentType), any(InputStream.class));
    }

    @Test
    void testQuotaReturns413() throws Exception {
        Long workspaceId = 1L;
        String filename = "too-big.bin";
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        byte[] content = new byte[16];

        when(workspaceFileFacade.createFromUpload(
            eq(workspaceId), eq(filename), eq(contentType), any(InputStream.class)))
                .thenThrow(new WorkspaceFileQuotaExceededException("Quota exceeded", 2048L, 1024L));

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        mockMvc
            .perform(multipart("/api/automation/internal/workspace-files/upload")
                .file(file)
                .param("workspaceId", workspaceId.toString()))
            .andExpect(status().isContentTooLarge())
            .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"))
            .andExpect(jsonPath("$.attempted").value(2048))
            .andExpect(jsonPath("$.limit").value(1024));
    }

    private static WorkspaceFile createWorkspaceFile(Long id, String name, String mimeType, long sizeBytes) {
        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setId(id);
        workspaceFile.setName(name);
        workspaceFile.setMimeType(mimeType);
        workspaceFile.setSizeBytes(sizeBytes);
        workspaceFile.setSource(WorkspaceFileSource.USER_UPLOAD);

        return workspaceFile;
    }
}
