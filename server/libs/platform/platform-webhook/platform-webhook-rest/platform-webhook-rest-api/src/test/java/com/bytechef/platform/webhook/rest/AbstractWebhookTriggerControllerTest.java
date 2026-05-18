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

package com.bytechef.platform.webhook.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that {@link AbstractWebhookTriggerController} emits signed tokens (v1.* format) rather than plain unsigned
 * FileEntry ids when converting {@link FileEntry}-shaped maps to public URLs in webhook responses.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AbstractWebhookTriggerControllerTest {

    private static final String PUBLIC_URL = "https://bytechef.example.com";
    private static final String SIGNED_TOKEN = "v1.9999999999.dGVzdA.AAAAAAAAAAAAAAAAAAAAAA";

    @Test
    void testConvertToFileEntryUrlEmitsSignedToken() {
        FileEntryTokens fileEntryTokens = mock(FileEntryTokens.class);

        when(fileEntryTokens.toSignedTokenIfConfigured(any(FileEntry.class))).thenReturn(Optional.of(SIGNED_TOKEN));

        TestableController controller = new TestableController(
            fileEntryTokens, PUBLIC_URL, mock(TempFileStorage.class));

        Map<String, Object> fileEntryMap = new LinkedHashMap<>();

        fileEntryMap.put("extension", "txt");
        fileEntryMap.put("mimeType", "text/plain");
        fileEntryMap.put("name", "test.txt");
        fileEntryMap.put("url", "store/test.txt");

        String url = controller.convertToFileEntryUrl(fileEntryMap);

        assertThat(url).isEqualTo(PUBLIC_URL + "/file-entries/" + SIGNED_TOKEN + "/content");
    }

    @Test
    void testConvertToFileEntryUrlTokenStartsWithV1() {
        FileEntryTokens fileEntryTokens = mock(FileEntryTokens.class);

        when(fileEntryTokens.toSignedTokenIfConfigured(any(FileEntry.class))).thenReturn(Optional.of(SIGNED_TOKEN));

        TestableController controller = new TestableController(
            fileEntryTokens, PUBLIC_URL, mock(TempFileStorage.class));

        Map<String, Object> fileEntryMap = new LinkedHashMap<>();

        fileEntryMap.put("extension", "pdf");
        fileEntryMap.put("mimeType", "application/pdf");
        fileEntryMap.put("name", "report.pdf");
        fileEntryMap.put("url", "store/report.pdf");

        String url = controller.convertToFileEntryUrl(fileEntryMap);
        String fileEntriesPrefix = "/file-entries/";
        String contentSuffix = "/content";
        int tokenStart = url.indexOf(fileEntriesPrefix) + fileEntriesPrefix.length();
        int tokenEnd = url.lastIndexOf(contentSuffix);
        String pathParam = url.substring(tokenStart, tokenEnd);

        assertThat(pathParam).startsWith("v1.");
    }

    @Test
    void testConvertToFileEntryUrlDoesNotEmitRawLegacyId() {
        FileEntryTokens fileEntryTokens = mock(FileEntryTokens.class);

        when(fileEntryTokens.toSignedTokenIfConfigured(any(FileEntry.class))).thenReturn(Optional.of(SIGNED_TOKEN));

        TestableController controller = new TestableController(
            fileEntryTokens, PUBLIC_URL, mock(TempFileStorage.class));

        Map<String, Object> fileEntryMap = new LinkedHashMap<>();

        fileEntryMap.put("extension", "json");
        fileEntryMap.put("mimeType", "application/json");
        fileEntryMap.put("name", "data.json");
        fileEntryMap.put("url", "store/data.json");

        @SuppressWarnings("unchecked")
        FileEntry fileEntry = new FileEntry((Map<String, ?>) (Map<?, ?>) fileEntryMap);
        String legacyId = fileEntry.toId();
        String url = controller.convertToFileEntryUrl(fileEntryMap);

        assertThat(url).doesNotContain(legacyId);
    }

    @Test
    void testConvertToFileEntryUrlFallsBackToLegacyIdWhenNoSecret() {
        FileEntryTokens fileEntryTokens = mock(FileEntryTokens.class);

        when(fileEntryTokens.toSignedTokenIfConfigured(any(FileEntry.class))).thenReturn(Optional.empty());

        TestableController controller = new TestableController(
            fileEntryTokens, PUBLIC_URL, mock(TempFileStorage.class));

        Map<String, Object> fileEntryMap = new LinkedHashMap<>();

        fileEntryMap.put("extension", "txt");
        fileEntryMap.put("mimeType", "text/plain");
        fileEntryMap.put("name", "fallback.txt");
        fileEntryMap.put("url", "store/fallback.txt");

        @SuppressWarnings("unchecked")
        FileEntry fileEntry = new FileEntry((Map<String, ?>) (Map<?, ?>) fileEntryMap);
        String legacyId = fileEntry.toId();
        String url = controller.convertToFileEntryUrl(fileEntryMap);

        assertThat(url).isEqualTo(PUBLIC_URL + "/file-entries/" + legacyId + "/content");
        assertThat(url).doesNotContain("v1.");
    }

    /**
     * Minimal concrete subclass in the same package, allowing direct invocation of the package-private
     * {@code convertToFileEntryUrl} method.
     */
    private static class TestableController extends AbstractWebhookTriggerController {

        TestableController(FileEntryTokens fileEntryTokens, String publicUrl, TempFileStorage tempFileStorage) {
            super(fileEntryTokens, publicUrl, tempFileStorage, null);
        }
    }
}
