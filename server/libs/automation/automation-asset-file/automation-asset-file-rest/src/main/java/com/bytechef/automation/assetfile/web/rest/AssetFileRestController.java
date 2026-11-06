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
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/internal/asset-files")
@PreAuthorize("isAuthenticated()")
@SuppressFBWarnings("EI")
public class AssetFileRestController {

    private static final Logger log = LoggerFactory.getLogger(AssetFileRestController.class);

    private final AssetFileFacade assetFileFacade;
    private final AssetFileMetrics assetFileMetrics;

    @SuppressFBWarnings("EI2")
    public AssetFileRestController(AssetFileFacade assetFileFacade, AssetFileMetrics assetFileMetrics) {
        this.assetFileFacade = assetFileFacade;
        this.assetFileMetrics = assetFileMetrics;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetFileDTO> upload(
        @RequestParam Long workspaceId, @RequestParam(defaultValue = "0") int environment,
        @RequestParam MultipartFile file) throws IOException {

        AssetFile created = assetFileFacade.createFromUpload(
            workspaceId, environment, file.getOriginalFilename(), file.getContentType(), file.getInputStream());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AssetFileDTO.from(created));
    }

    @GetMapping(value = "/{id}/content")
    public ResponseEntity<StreamingResponseBody> download(
        @PathVariable Long id, @RequestParam(required = false) String disposition) {

        AssetFile assetFile = assetFileFacade.findById(id);

        Long workspaceId = assetFile.getWorkspaceId();

        StreamingResponseBody body = out -> {
            try (InputStream in = assetFileFacade.downloadContent(id)) {
                in.transferTo(out);
            } catch (IOException ioException) {
                log.warn(
                    "Asset file content stream failed mid-transfer for file id={} workspaceId={}; the response status "
                        + "was already committed so the client will see a truncated download.",
                    id, workspaceId, ioException);
                assetFileMetrics.recordStreamFailure(ioException.getClass()
                    .getSimpleName());

                throw ioException;
            }
        };

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, resolveContentDisposition(assetFile, disposition))
            .header(HttpHeaders.CONTENT_TYPE, assetFile.getMimeType())
            .body(body);
    }

    private static String resolveContentDisposition(AssetFile assetFile, String dispositionParam) {
        String mimeType = assetFile.getMimeType();
        boolean inlineSafe = mimeType != null
            && (mimeType.equals(MediaType.APPLICATION_PDF_VALUE) || mimeType.startsWith("image/"));

        String effective = "inline".equalsIgnoreCase(dispositionParam) && inlineSafe ? "inline" : "attachment";

        return "%s; filename=\"%s\"".formatted(effective, assetFile.getName());
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetFileDTO> replaceContent(
        @PathVariable Long id, @RequestParam MultipartFile file) throws IOException {

        AssetFile updated = assetFileFacade.updateContent(id, file.getContentType(), file.getInputStream());

        return ResponseEntity.ok(AssetFileDTO.from(updated));
    }

    @ExceptionHandler(AssetFileQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(AssetFileQuotaExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
            .body(new ErrorResponse(
                "QUOTA_EXCEEDED", exception.getMessage(), exception.getAttempted(), exception.getLimit()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        long limit = exception.getMaxUploadSize();

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
            .body(new ErrorResponse(
                "MAX_UPLOAD_SIZE_EXCEEDED",
                "Uploaded file exceeds the configured multipart size limit.",
                -1L,
                limit));
    }

    @ExceptionHandler(AssetFileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AssetFileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", exception.getMessage()));
    }

    public record ErrorResponse(String code, String message, long attempted, long limit) {
    }
}
