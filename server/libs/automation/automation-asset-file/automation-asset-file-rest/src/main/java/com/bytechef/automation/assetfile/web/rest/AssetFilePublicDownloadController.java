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
import com.bytechef.automation.assetfile.service.AssetFileSystemFacade;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/asset-files")
@SuppressFBWarnings("EI")
public class AssetFilePublicDownloadController {

    private static final Logger log = LoggerFactory.getLogger(AssetFilePublicDownloadController.class);

    private final AssetFileSystemFacade assetFileSystemFacade;
    private final AssetFileFileStorage assetFileFileStorage;

    @SuppressFBWarnings("EI2")
    public AssetFilePublicDownloadController(
        AssetFileSystemFacade assetFileSystemFacade, AssetFileFileStorage assetFileFileStorage) {

        this.assetFileSystemFacade = assetFileSystemFacade;
        this.assetFileFileStorage = assetFileFileStorage;
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<StreamingResponseBody> downloadPublic(@PathVariable String token) {
        Optional<AssetFile> assetFile = assetFileSystemFacade.fetchByPublicLinkToken(token);

        if (assetFile.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        AssetFile resolved = assetFile.get();

        return streamResponse(resolved.getFile(), resolved.getName(), resolved.getMimeType());
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

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
            .header(
                HttpHeaders.CONTENT_TYPE,
                mimeType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mimeType)
            .header("X-Content-Type-Options", "nosniff")
            .body(body);
    }
}
