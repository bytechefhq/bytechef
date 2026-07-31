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

package com.bytechef.automation.assetfile.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.web.rest.config.AutomationAssetFileRestTestConfiguration;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link AssetFileRestController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationAssetFileRestTestConfiguration.class)
@WebMvcTest(value = AssetFileRestController.class)
@WithMockUser
class AssetFileRestControllerIntTest {

    private static final long CALLER_USER_ID = 7L;
    private static final long ALLOWED_WORKSPACE_ID = 1L;
    private static final long FOREIGN_WORKSPACE_ID = 99L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetFileFacade assetFileFacade;

    // Not exercised here, but the test configuration's component scan also constructs
    // AssetFilePublicDownloadController, which requires this bean.
    @MockitoBean
    private AssetFileFileStorage assetFileFileStorage;

    @MockitoBean
    private AssetFileMetrics assetFileMetrics;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void setUpAuth() {
        User user = new User();

        user.setId(CALLER_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace allowed = new Workspace();

        allowed.setId(ALLOWED_WORKSPACE_ID);

        when(workspaceFacade.getUserWorkspaces(CALLER_USER_ID)).thenReturn(List.of(allowed));
    }

    @Test
    void testUploadReturns201AndDto() throws Exception {
        Long workspaceId = ALLOWED_WORKSPACE_ID;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] content = "# Heading".getBytes(StandardCharsets.UTF_8);

        AssetFile created = createAssetFile(42L, filename, contentType, content.length);

        when(assetFileFacade.createFromUpload(
            eq(workspaceId), anyInt(), eq(filename), eq(contentType), any(InputStream.class))).thenReturn(created);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        mockMvc
            .perform(multipart("/api/automation/internal/asset-files/upload")
                .file(file)
                .param("workspaceId", workspaceId.toString())
                .with(csrfRequest -> {
                    csrfRequest.setMethod("POST");

                    return csrfRequest;
                }))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.name").value(filename))
            .andExpect(jsonPath("$.mimeType").value(contentType))
            .andExpect(jsonPath("$.source").value(AssetFileSource.USER_UPLOAD.name()));

        verify(assetFileFacade).createFromUpload(
            eq(workspaceId), anyInt(), eq(filename), eq(contentType), any(InputStream.class));
    }

    @Test
    void testUploadForForeignWorkspaceReturns403() throws Exception {
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] content = "# Heading".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        mockMvc
            .perform(multipart("/api/automation/internal/asset-files/upload")
                .file(file)
                .param("workspaceId", String.valueOf(FOREIGN_WORKSPACE_ID))
                .with(csrfRequest -> {
                    csrfRequest.setMethod("POST");

                    return csrfRequest;
                }))
            .andExpect(status().isForbidden());

        verify(assetFileFacade, never()).createFromUpload(
            eq(FOREIGN_WORKSPACE_ID), anyInt(), eq(filename), eq(contentType), any(InputStream.class));
    }

    @Test
    void testDownloadReturnsContent() throws Exception {
        Long id = 42L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] contentBytes = "# Downloaded".getBytes(StandardCharsets.UTF_8);

        AssetFile assetFile = createAssetFile(id, filename, contentType, contentBytes.length);

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(ALLOWED_WORKSPACE_ID);
        when(assetFileFacade.findById(id)).thenReturn(assetFile);
        when(assetFileFacade.downloadContent(id)).thenReturn(new ByteArrayInputStream(contentBytes));

        MvcResult asyncResult = mockMvc
            .perform(get("/api/automation/internal/asset-files/{id}/content", id))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc
            .perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"spec.md\""))
            .andExpect(content().bytes(contentBytes));
    }

    @Test
    void testDownloadPdfWithInlineDispositionReturnsInlineHeader() throws Exception {
        Long id = 43L;
        String filename = "report.pdf";
        String contentType = MediaType.APPLICATION_PDF_VALUE;
        byte[] contentBytes = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

        AssetFile assetFile = createAssetFile(id, filename, contentType, contentBytes.length);

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(ALLOWED_WORKSPACE_ID);
        when(assetFileFacade.findById(id)).thenReturn(assetFile);
        when(assetFileFacade.downloadContent(id)).thenReturn(new ByteArrayInputStream(contentBytes));

        MvcResult asyncResult = mockMvc
            .perform(get("/api/automation/internal/asset-files/{id}/content", id).param("disposition", "inline"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc
            .perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "inline; filename=\"report.pdf\""));
    }

    @Test
    void testDownloadUnsafeMimeWithInlineDispositionFallsBackToAttachment() throws Exception {
        Long id = 44L;
        String filename = "index.html";
        String contentType = MediaType.TEXT_HTML_VALUE;
        byte[] contentBytes = "<html></html>".getBytes(StandardCharsets.UTF_8);

        AssetFile assetFile = createAssetFile(id, filename, contentType, contentBytes.length);

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(ALLOWED_WORKSPACE_ID);
        when(assetFileFacade.findById(id)).thenReturn(assetFile);
        when(assetFileFacade.downloadContent(id)).thenReturn(new ByteArrayInputStream(contentBytes));

        MvcResult asyncResult = mockMvc
            .perform(get("/api/automation/internal/asset-files/{id}/content", id).param("disposition", "inline"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc
            .perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"index.html\""));
    }

    @Test
    void testDownloadForeignWorkspaceReturns404() throws Exception {
        Long id = 42L;

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(FOREIGN_WORKSPACE_ID);

        mockMvc
            .perform(get("/api/automation/internal/asset-files/{id}/content", id))
            .andExpect(status().isNotFound());

        verify(assetFileFacade, never()).downloadContent(eq(id));
    }

    @Test
    void testPutContentUpdates() throws Exception {
        Long id = 42L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] updatedBytes = "# Updated".getBytes(StandardCharsets.UTF_8);

        AssetFile updated = createAssetFile(id, filename, contentType, updatedBytes.length);

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(ALLOWED_WORKSPACE_ID);
        when(assetFileFacade.updateContent(eq(id), eq(contentType), any(InputStream.class))).thenReturn(updated);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, updatedBytes);

        mockMvc
            .perform(multipart("/api/automation/internal/asset-files/{id}/content", id)
                .file(file)
                .with(multipartRequest -> {
                    multipartRequest.setMethod("PUT");

                    return multipartRequest;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.sizeBytes").value(updatedBytes.length));

        verify(assetFileFacade).updateContent(eq(id), eq(contentType), any(InputStream.class));
    }

    @Test
    void testPutContentForForeignWorkspaceReturns404() throws Exception {
        Long id = 42L;
        String filename = "spec.md";
        String contentType = "text/markdown";
        byte[] updatedBytes = "# Updated".getBytes(StandardCharsets.UTF_8);

        when(assetFileFacade.getOwningWorkspaceId(id)).thenReturn(FOREIGN_WORKSPACE_ID);

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, updatedBytes);

        mockMvc
            .perform(multipart("/api/automation/internal/asset-files/{id}/content", id)
                .file(file)
                .with(multipartRequest -> {
                    multipartRequest.setMethod("PUT");

                    return multipartRequest;
                }))
            .andExpect(status().isNotFound());

        verify(assetFileFacade, never()).updateContent(eq(id), eq(contentType), any(InputStream.class));
    }

    @Test
    void testQuotaReturns413() throws Exception {
        Long workspaceId = ALLOWED_WORKSPACE_ID;
        String filename = "too-big.bin";
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        byte[] content = new byte[16];

        when(assetFileFacade.createFromUpload(
            eq(workspaceId), anyInt(), eq(filename), eq(contentType), any(InputStream.class)))
                .thenThrow(new AssetFileQuotaExceededException("Quota exceeded", 2048L, 1024L));

        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        mockMvc
            .perform(multipart("/api/automation/internal/asset-files/upload")
                .file(file)
                .param("workspaceId", workspaceId.toString())
                .with(csrfRequest -> {
                    csrfRequest.setMethod("POST");

                    return csrfRequest;
                }))
            .andExpect(status().isContentTooLarge())
            .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"))
            .andExpect(jsonPath("$.attempted").value(2048))
            .andExpect(jsonPath("$.limit").value(1024));
    }

    private static AssetFile createAssetFile(Long id, String name, String mimeType, long sizeBytes) {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(id);
        assetFile.setName(name);
        assetFile.setMimeType(mimeType);
        assetFile.setSizeBytes(sizeBytes);
        assetFile.setSource(AssetFileSource.USER_UPLOAD);

        return assetFile;
    }
}
