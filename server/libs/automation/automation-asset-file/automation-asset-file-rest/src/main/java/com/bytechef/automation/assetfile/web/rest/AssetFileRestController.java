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
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.user.service.UserService;
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
import org.springframework.web.server.ResponseStatusException;
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
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI2")
    public AssetFileRestController(
        AssetFileFacade assetFileFacade, AssetFileMetrics assetFileMetrics, UserService userService,
        WorkspaceFacade workspaceFacade) {

        this.assetFileFacade = assetFileFacade;
        this.assetFileMetrics = assetFileMetrics;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetFileDTO> upload(
        @RequestParam Long workspaceId, @RequestParam(defaultValue = "0") int environment,
        @RequestParam MultipartFile file) throws IOException {

        verifyUserCanAccessWorkspaceForUpload(workspaceId);

        AssetFile created = assetFileFacade.createFromUpload(
            workspaceId, environment, file.getOriginalFilename(), file.getContentType(), file.getInputStream());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AssetFileDTO.from(created));
    }

    @GetMapping(value = "/{id}/content")
    public ResponseEntity<StreamingResponseBody> download(
        @PathVariable Long id, @RequestParam(required = false) String disposition) {

        Long workspaceId = verifyUserCanAccessFile(id);

        AssetFile assetFile = assetFileFacade.findById(id);

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

        verifyUserCanAccessFile(id);

        AssetFile updated = assetFileFacade.updateContent(id, file.getContentType(), file.getInputStream());

        return ResponseEntity.ok(AssetFileDTO.from(updated));
    }

    @ExceptionHandler(AssetFileQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(AssetFileQuotaExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
            .body(new ErrorResponse(
                "QUOTA_EXCEEDED", exception.getMessage(), exception.getAttempted(), exception.getLimit()));
    }

    /**
     * Surfaces Spring's multipart-size violation as a structured 413 instead of the framework default (an empty-body
     * 500). Without this, the client's parseServerError sees only "Upload failed: 500 " and the user has no idea why.
     */
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

    /**
     * Confirms the calling user is a member of {@code workspaceId} when the workspace id was supplied directly by the
     * caller (upload). Returns 403 because the caller is asserting "I want to upload to this workspace" and a 404 would
     * misrepresent the failure as "workspace does not exist". Sibling controllers in the EE module use the same shape
     * via {@code WorkspaceAccessGuard}, which throws a {@code ForbiddenException} that maps to 403; this module has no
     * equivalent domain exception so {@link ResponseStatusException} is used directly. The companion
     * {@link #verifyUserCanAccessFile(long)} keeps the 404 shape because there the file id is the lookup key and
     * distinguishing "missing" from "wrong workspace" would let a caller enumerate ids.
     */
    private void verifyUserCanAccessWorkspaceForUpload(long workspaceId) {
        long userId = userService.getCurrentUser()
            .getId();

        if (!isUserWorkspaceMember(userId, workspaceId)) {
            log.warn(
                "AssetFileRestController returning 403 (security-audit event): user {} attempted to upload to workspace",
                userId);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace not accessible");
        }
    }

    /**
     * Resolves the workspace owning {@code fileId}, verifies the caller is a member, and returns the workspace id so
     * the caller can avoid a second {@link AssetFileFacade#getOwningWorkspaceId(Long)} round-trip. Throws
     * {@link AssetFileNotFoundException} (→ 404) for both "file does not exist" and "file exists in another workspace"
     * — the two cases are intentionally indistinguishable so workspace members cannot enumerate file ids belonging to
     * other workspaces.
     */
    private Long verifyUserCanAccessFile(long fileId) {
        Long workspaceId = assetFileFacade.getOwningWorkspaceId(fileId);

        long userId = userService.getCurrentUser()
            .getId();

        if (!isUserWorkspaceMember(userId, workspaceId)) {
            log.warn(
                "AssetFileRestController returning 404 (security-audit event): user {} attempted to access foreign file",
                userId);

            throw new AssetFileNotFoundException("Asset file not accessible");
        }

        return workspaceId;
    }

    private boolean isUserWorkspaceMember(long userId, long workspaceId) {
        return workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);
    }

    public record ErrorResponse(String code, String message, long attempted, long limit) {
    }
}
