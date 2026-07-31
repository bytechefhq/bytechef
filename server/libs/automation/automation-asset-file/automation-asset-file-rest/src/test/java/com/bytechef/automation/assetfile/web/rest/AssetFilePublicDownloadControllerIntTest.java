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
import com.bytechef.automation.assetfile.web.rest.config.AutomationAssetFileRestTestConfiguration;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link AssetFilePublicDownloadController}. Anonymous permit-all wiring lives in the central
 * security configuration (via {@code AssetFileAuthorizeHttpRequestContributor}), outside this slice — the tests focus
 * on token resolution semantics and the uniform-404 contract.
 *
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
    private FileEntryTokens fileEntryTokens;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @Test
    void testPublicDownloadStreamsContentAsAttachment() throws Exception {
        FileEntry fileEntry = new FileEntry("report.html", "asset_files/report.html");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(5L);
        assetFile.setName("report.html");
        assetFile.setMimeType("text/html");
        assetFile.setFile(fileEntry);

        when(assetFileFacade.fetchByPublicLinkToken("good-token")).thenReturn(Optional.of(assetFile));
        when(assetFileFileStorage.getInputStream(fileEntry))
            .thenReturn(new ByteArrayInputStream("<h1>hi</h1>".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/public/good-token"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            // Attachment is unconditional on the anonymous surface — inline text/html would be an XSS vector.
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.html\""))
            .andExpect(header().string("Content-Type", "text/html"))
            .andExpect(content().string("<h1>hi</h1>"));
    }

    @Test
    void testPublicDownloadReturns404ForUnknownToken() throws Exception {
        when(assetFileFacade.fetchByPublicLinkToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/automation/asset-files/public/bad-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testSignedDownloadStreamsContent() throws Exception {
        FileEntry fileEntry = new FileEntry("data.csv", "asset_files/data.csv");

        when(fileEntryTokens.parseSignedToken("v1.99.payload.sig")).thenReturn(Optional.of(fileEntry));
        when(assetFileFileStorage.getInputStream(fileEntry))
            .thenReturn(new ByteArrayInputStream("a,b".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/signed/v1.99.payload.sig"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"data.csv\""))
            .andExpect(content().string("a,b"));
    }

    @Test
    void testSignedDownloadReturns404ForInvalidToken() throws Exception {
        when(fileEntryTokens.parseSignedToken("v1.expired.payload.sig")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/automation/asset-files/signed/v1.expired.payload.sig"))
            .andExpect(status().isNotFound());
    }
}
