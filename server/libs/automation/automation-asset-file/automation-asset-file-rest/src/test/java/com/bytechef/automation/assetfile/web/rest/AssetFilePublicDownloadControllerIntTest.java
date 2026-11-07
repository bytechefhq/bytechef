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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.file.storage.token.FileEntryTokensImpl;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@Import(AssetFilePublicDownloadControllerIntTest.FileEntryTokensTestConfiguration.class)
@WebMvcTest(value = AssetFilePublicDownloadController.class)
@WithMockUser
class AssetFilePublicDownloadControllerIntTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtYmFzZTY0LWVuY29kZWQ=";
    private static final String OTHER_SECRET = "b3RoZXItc2VjcmV0LWZvci1zaWduZWQtZG93bmxvYWRzLXRlc3Q=";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

    private static final FileEntry FILE_A = new FileEntry("a.csv", "asset_files/a.csv");
    private static final FileEntry FILE_B = new FileEntry("b.csv", "asset_files/b.csv");

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

    @Autowired
    private FileEntryTokens fileEntryTokens;

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

    @Test
    @WithAnonymousUser
    void testSignedDownloadStreamsContentForAValidToken() throws Exception {
        String token = fileEntryTokens.toSignedToken(FILE_A);

        when(assetFileFileStorage.getInputStream(FILE_A))
            .thenReturn(new ByteArrayInputStream("a,b".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/signed/{token}", token))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"a.csv\""))
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().string("a,b"));

        verify(assetFileFileStorage).getInputStream(FILE_A);
        verifyNoInteractions(assetFileFacade, assetFileSystemFacade);
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadReturns404ForAnExpiredToken() throws Exception {
        Clock pastClock = Clock.fixed(Instant.now()
            .minus(Duration.ofHours(1)), ZoneOffset.UTC);

        FileEntryTokens pastSigner = new FileEntryTokensImpl(pastClock, SECRET, List.of(), TTL, CLOCK_SKEW);

        String token = pastSigner.toSignedToken(FILE_A);

        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", token))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadReturns404ForATamperedSignature() throws Exception {
        String[] parts = fileEntryTokens.toSignedToken(FILE_A)
            .split("\\.");

        String signature = parts[3];
        char firstCharacter = signature.charAt(0);
        String tamperedSignature = (firstCharacter == 'A' ? 'B' : 'A') + signature.substring(1);

        String tampered = String.join(".", parts[0], parts[1], parts[2], tamperedSignature);

        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", tampered))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadReturns404ForATokenSignedWithAnotherSecret() throws Exception {
        FileEntryTokens foreignSigner = new FileEntryTokensImpl(
            Clock.systemUTC(), OTHER_SECRET, List.of(), TTL, CLOCK_SKEW);

        String token = foreignSigner.toSignedToken(FILE_A);

        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", token))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadReturns404ForAnExtendedExpiry() throws Exception {
        String[] parts = fileEntryTokens.toSignedToken(FILE_A)
            .split("\\.");

        long extendedExpiry = Long.parseLong(parts[1]) + Duration.ofDays(365)
            .toSeconds();

        String tampered = String.join(".", parts[0], String.valueOf(extendedExpiry), parts[2], parts[3]);

        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", tampered))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadTokenForOneFileCannotFetchAnother() throws Exception {
        String[] tokenAParts = fileEntryTokens.toSignedToken(FILE_A)
            .split("\\.");
        String[] tokenBParts = fileEntryTokens.toSignedToken(FILE_B)
            .split("\\.");

        String spliced = String.join(".", tokenAParts[0], tokenAParts[1], tokenBParts[2], tokenAParts[3]);

        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", spliced))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());

        when(assetFileFileStorage.getInputStream(FILE_A))
            .thenReturn(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc
            .perform(get("/api/automation/asset-files/signed/{token}", String.join(".", tokenAParts)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"a.csv\""))
            .andExpect(content().string("a"));

        verify(assetFileFileStorage).getInputStream(FILE_A);
        verify(assetFileFileStorage, never()).getInputStream(FILE_B);
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadReturns404ForAMalformedToken() throws Exception {
        mockMvc.perform(get("/api/automation/asset-files/signed/{token}", "not-a-token"))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @Test
    @WithAnonymousUser
    void testSignedDownloadWithoutATokenDoesNotStreamAnything() throws Exception {
        mockMvc.perform(get("/api/automation/asset-files/signed/"))
            .andExpect(status().isNotFound());

        verify(assetFileFileStorage, never()).getInputStream(any());
    }

    @TestConfiguration
    static class FileEntryTokensTestConfiguration {

        @Bean
        FileEntryTokens fileEntryTokens() {
            return new FileEntryTokensImpl(Clock.systemUTC(), SECRET, List.of(), TTL, CLOCK_SKEW);
        }
    }
}
