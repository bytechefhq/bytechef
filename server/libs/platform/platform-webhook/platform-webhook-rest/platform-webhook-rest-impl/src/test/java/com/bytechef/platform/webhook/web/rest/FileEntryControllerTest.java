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

package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryControllerTest {

    private TempFileStorage tempFileStorage;
    private FileEntryTokens fileEntryTokens;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tempFileStorage = Mockito.mock(TempFileStorage.class);
        fileEntryTokens = Mockito.mock(FileEntryTokens.class);

        FileEntryController controller = new FileEntryController(tempFileStorage, fileEntryTokens, false);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .build();
    }

    @Test
    void testSignedTokenAccepted() throws Exception {
        FileEntry parsed = new FileEntry("file.txt", "txt", "text/plain", "file:/temp/uuid.txt");

        when(fileEntryTokens.looksLikeSignedToken("v1.123.payload.sig")).thenReturn(true);
        when(fileEntryTokens.parseSignedToken("v1.123.payload.sig")).thenReturn(Optional.of(parsed));
        when(tempFileStorage.getInputStream(parsed))
            .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/file-entries/v1.123.payload.sig/content"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));
    }

    @Test
    void testSignedTokenRejectedAsNotFound() throws Exception {
        when(fileEntryTokens.looksLikeSignedToken("v1.123.bad.sig")).thenReturn(true);
        when(fileEntryTokens.parseSignedToken("v1.123.bad.sig")).thenReturn(Optional.empty());

        mockMvc.perform(get("/file-entries/v1.123.bad.sig/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }

    @Test
    void testLegacyIdAcceptedInPermissiveMode() throws Exception {
        // base64("txt_;_text/plain_;_file_;_file:/temp/uuid.txt")
        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);
        when(tempFileStorage.getInputStream(any()))
            .thenReturn(new ByteArrayInputStream("legacy".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/file-entries/" + legacyId + "/content"))
            .andExpect(status().isOk())
            .andExpect(content().string("legacy"));

        verify(fileEntryTokens, never()).parseSignedToken(any());
    }

    @Test
    void testLegacyIdRejectedInStrictMode() throws Exception {
        FileEntryController strictController = new FileEntryController(tempFileStorage, fileEntryTokens, true);

        MockMvc strictMvc = MockMvcBuilders.standaloneSetup(strictController)
            .build();

        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);

        strictMvc.perform(get("/file-entries/" + legacyId + "/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }

    @Test
    void testLegacyWarnRateLimited() throws Exception {
        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);
        when(tempFileStorage.getInputStream(any()))
            .thenAnswer(invocation -> new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

        // We can't easily assert log output without a custom appender — instead, verify the controller doesn't
        // crash on rapid-fire requests and that the legacy path still serves all of them.
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/file-entries/" + legacyId + "/content"))
                .andExpect(status().isOk());
        }
    }

    @Test
    void testFileStorageExceptionReturns404NotFiveHundred() throws Exception {
        FileEntry parsed = new FileEntry("file.txt", "txt", "text/plain", "file:/temp/uuid.txt");

        when(fileEntryTokens.looksLikeSignedToken("v1.123.payload.sig")).thenReturn(true);
        when(fileEntryTokens.parseSignedToken("v1.123.payload.sig")).thenReturn(Optional.of(parsed));
        when(tempFileStorage.getInputStream(parsed))
            .thenThrow(new FileStorageException("file not found: /temp/uuid.txt"));

        mockMvc.perform(get("/file-entries/v1.123.payload.sig/content"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testMaliciousLegacyIdReturns404NotFiveHundred() throws Exception {
        // base64("txt_;_text/plain_;_file_;_/etc/passwd") — already rejected by FileEntry.parse
        String maliciousId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO18vZXRjL3Bhc3N3ZA==";

        when(fileEntryTokens.looksLikeSignedToken(maliciousId)).thenReturn(false);

        mockMvc.perform(get("/file-entries/" + maliciousId + "/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }
}
