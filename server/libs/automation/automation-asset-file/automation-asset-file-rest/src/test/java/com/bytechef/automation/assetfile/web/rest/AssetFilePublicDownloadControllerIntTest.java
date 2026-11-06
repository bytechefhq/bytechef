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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.service.AssetFileSystemFacade;
import com.bytechef.automation.assetfile.web.rest.config.AutomationAssetFileRestTestConfiguration;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationAssetFileRestTestConfiguration.class)
@WebMvcTest(value = AssetFilePublicDownloadController.class)
@WithMockUser
class AssetFilePublicDownloadControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetFileFacade assetFileFacade;

    @MockitoBean
    private AssetFileFileStorage assetFileFileStorage;

    @MockitoBean
    private AssetFileMetrics assetFileMetrics;

    @MockitoBean
    private AssetFileSystemFacade assetFileSystemFacade;

    @Test
    void testPublicDownloadStreamsContentAsAttachment() throws Exception {
        FileEntry fileEntry = new FileEntry("report.html", "asset_files/report.html");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(5L);
        assetFile.setName("report.html");
        assetFile.setMimeType("text/html");
        assetFile.setFile(fileEntry);

        when(assetFileSystemFacade.fetchByPublicLinkToken("good-token")).thenReturn(Optional.of(assetFile));
        when(assetFileFileStorage.getInputStream(fileEntry))
            .thenReturn(new ByteArrayInputStream("<h1>hi</h1>".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/public/good-token"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.html\""))
            .andExpect(header().string("Content-Type", "text/html"))
            .andExpect(content().string("<h1>hi</h1>"));
    }

    @Test
    void testPublicDownloadReturns404ForUnknownToken() throws Exception {
        when(assetFileSystemFacade.fetchByPublicLinkToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/automation/asset-files/public/bad-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void testPublicDownloadResolvesThroughSystemFacadeForAnonymousCaller() throws Exception {
        FileEntry fileEntry = new FileEntry("notes.txt", "asset_files/notes.txt");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(7L);
        assetFile.setName("notes.txt");
        assetFile.setMimeType("text/plain");
        assetFile.setFile(fileEntry);

        when(assetFileSystemFacade.fetchByPublicLinkToken("anon-token")).thenReturn(Optional.of(assetFile));
        when(assetFileFileStorage.getInputStream(fileEntry))
            .thenReturn(new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/public/anon-token"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string("hi"));

        verify(assetFileSystemFacade).fetchByPublicLinkToken("anon-token");
    }
}
