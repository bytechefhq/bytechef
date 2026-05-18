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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves files produced by public webhook executions. The endpoint is intentionally unauthenticated — webhook callers
 * are themselves unauthenticated — so capability-by-knowledge has been the de facto access control.
 * <p>
 * As of the 2026-05-18 signing rollout, the preferred form is an HMAC-signed token (see {@link FileEntryTokens}); the
 * legacy unsigned-id form remains accepted while {@code bytechef.file-storage.signed-url.required} is {@code false}.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class FileEntryController {

    private static final Logger log = LoggerFactory.getLogger(FileEntryController.class);
    private static final Marker LEGACY_MARKER = MarkerFactory.getMarker("FILE_ENTRY_LEGACY");
    private static final long LEGACY_WARN_INTERVAL_MS = 60_000L;

    private final TempFileStorage tempFileStorage;
    private final FileEntryTokens fileEntryTokens;
    private final boolean signedRequired;
    private final AtomicLong lastLegacyWarnEpochMs = new AtomicLong();

    public FileEntryController(
        TempFileStorage tempFileStorage, FileEntryTokens fileEntryTokens,
        @Value("${bytechef.file-storage.signed-url.required:false}") boolean signedRequired) {

        this.tempFileStorage = tempFileStorage;
        this.fileEntryTokens = fileEntryTokens;
        this.signedRequired = signedRequired;
    }

    @GetMapping("/file-entries/{id}/content")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getFileEntryContent(@PathVariable("id") String id) {
        Optional<FileEntry> fileEntry = resolve(id);

        if (fileEntry.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        FileEntry resolved = fileEntry.get();

        try {
            return ResponseEntity.ok()
                .contentType(
                    resolved.getMimeType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.asMediaType(MimeType.valueOf(resolved.getMimeType())))
                .body(new InputStreamResource(tempFileStorage.getInputStream(resolved)));
        } catch (FileStorageException e) {
            log.debug("File not accessible for entry; returning 404", e);

            return ResponseEntity.notFound()
                .build();
        }
    }

    private Optional<FileEntry> resolve(String id) {
        if (fileEntryTokens.looksLikeSignedToken(id)) {
            return fileEntryTokens.parseSignedToken(id);
        }

        if (signedRequired) {
            return Optional.empty();
        }

        maybeLogLegacyAccess();

        try {
            return Optional.of(FileEntry.parse(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void maybeLogLegacyAccess() {
        long now = System.currentTimeMillis();
        long last = lastLegacyWarnEpochMs.get();

        if (now - last >= LEGACY_WARN_INTERVAL_MS && lastLegacyWarnEpochMs.compareAndSet(last, now)) {
            log.warn(LEGACY_MARKER,
                "Accepted legacy unsigned file-entry id. Migrate clients to signed URLs; this path will be removed.");
        }
    }
}
