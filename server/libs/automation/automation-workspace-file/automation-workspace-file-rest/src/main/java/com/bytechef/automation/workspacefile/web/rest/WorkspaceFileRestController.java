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

package com.bytechef.automation.workspacefile.web.rest;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/internal/workspace-files")
@SuppressFBWarnings("EI")
public class WorkspaceFileRestController {

    private final WorkspaceFileFacade workspaceFileFacade;

    @SuppressFBWarnings("EI2")
    public WorkspaceFileRestController(WorkspaceFileFacade workspaceFileFacade) {
        this.workspaceFileFacade = workspaceFileFacade;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkspaceFileDTO> upload(
        @RequestParam Long workspaceId, @RequestParam MultipartFile file) throws IOException {

        WorkspaceFile created = workspaceFileFacade.createFromUpload(
            workspaceId, file.getOriginalFilename(), file.getContentType(), file.getInputStream());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(WorkspaceFileDTO.from(created));
    }

    @GetMapping(value = "/{id}/content")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
        WorkspaceFile workspaceFile = workspaceFileFacade.findById(id);

        StreamingResponseBody body = out -> {
            try (InputStream in = workspaceFileFacade.downloadContent(id)) {
                in.transferTo(out);
            }
        };

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"%s\"".formatted(workspaceFile.getName()))
            .header(HttpHeaders.CONTENT_TYPE, workspaceFile.getMimeType())
            .body(body);
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkspaceFileDTO> replaceContent(
        @PathVariable Long id, @RequestParam MultipartFile file) throws IOException {

        WorkspaceFile updated = workspaceFileFacade.updateContent(id, file.getContentType(), file.getInputStream());

        return ResponseEntity.ok(WorkspaceFileDTO.from(updated));
    }

    @ExceptionHandler(WorkspaceFileQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(WorkspaceFileQuotaExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
            .body(new ErrorResponse(
                "QUOTA_EXCEEDED", exception.getMessage(), exception.getAttempted(), exception.getLimit()));
    }

    public record ErrorResponse(String code, String message, long attempted, long limit) {
    }
}
