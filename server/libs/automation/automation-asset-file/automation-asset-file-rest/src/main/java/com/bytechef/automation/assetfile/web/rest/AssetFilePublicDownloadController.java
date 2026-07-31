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

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.token.FileEntryTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Serves asset file content to ANONYMOUS callers holding a capability token. Two token families:
 *
 * <ul>
 * <li>{@code /public/{token}} — the durable per-file public link enabled by a workspace member. Resolves only while the
 * file's link is enabled AND the operator-level {@code bytechef.asset-file.sharing.public-link-enabled} kill-switch is
 * on.</li>
 * <li>{@code /signed/{token}} — a short-lived HMAC-signed token minted via
 * {@link AssetFileFacade#createSignedDownloadToken}. Expires with the platform signed-URL TTL.</li>
 * </ul>
 *
 * Every failure mode maps to a uniform 404 — an anonymous caller must not be able to distinguish "no such token",
 * "expired", or "sharing disabled".
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/asset-files")
@SuppressFBWarnings("EI")
public class AssetFilePublicDownloadController {

    private static final Logger log = LoggerFactory.getLogger(AssetFilePublicDownloadController.class);

    private final AssetFileFacade assetFileFacade;
    private final AssetFileFileStorage assetFileFileStorage;
    private final ObjectProvider<FileEntryTokens> fileEntryTokensObjectProvider;

    @SuppressFBWarnings("EI2")
    public AssetFilePublicDownloadController(
        AssetFileFacade assetFileFacade, AssetFileFileStorage assetFileFileStorage,
        ObjectProvider<FileEntryTokens> fileEntryTokensObjectProvider) {

        this.assetFileFacade = assetFileFacade;
        this.assetFileFileStorage = assetFileFileStorage;
        this.fileEntryTokensObjectProvider = fileEntryTokensObjectProvider;
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<StreamingResponseBody> downloadPublic(@PathVariable String token) {
        Optional<AssetFile> assetFile = assetFileFacade.fetchByPublicLinkToken(token);

        if (assetFile.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        AssetFile resolved = assetFile.get();

        return streamResponse(resolved.getFile(), resolved.getName(), resolved.getMimeType());
    }

    @GetMapping("/signed/{token}")
    public ResponseEntity<StreamingResponseBody> downloadSigned(@PathVariable String token) {
        FileEntryTokens fileEntryTokens = fileEntryTokensObjectProvider.getIfAvailable();

        if (fileEntryTokens == null) {
            return ResponseEntity.notFound()
                .build();
        }

        Optional<FileEntry> fileEntry = fileEntryTokens.parseSignedToken(token);

        return fileEntry
            .map(resolved -> streamResponse(resolved, resolved.getName(), resolved.getMimeType()))
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    private ResponseEntity<StreamingResponseBody> streamResponse(
        FileEntry fileEntry, String filename, String mimeType) {

        StreamingResponseBody body = out -> {
            try (InputStream in = assetFileFileStorage.getInputStream(fileEntry)) {
                in.transferTo(out);
            } catch (IOException | FileStorageException exception) {
                log.warn("Asset file public download stream failed for {}", filename, exception);

                if (exception instanceof IOException ioException) {
                    throw ioException;
                }

                throw new IOException(exception);
            }
        };

        // Force attachment unconditionally: this surface is anonymous, so rendering caller-controlled content
        // (notably text/html) inline on the application origin would be a stored-XSS vector. Even images and PDFs
        // gain nothing from inline here — a shared file's purpose is download.
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
            .header(
                HttpHeaders.CONTENT_TYPE,
                mimeType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mimeType)
            .header("X-Content-Type-Options", "nosniff")
            .body(body);
    }
}
