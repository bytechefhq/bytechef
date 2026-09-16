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

package com.bytechef.platform.knowledgebase.web.rest;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal/knowledge-bases")
@SuppressFBWarnings("EI")
class KnowledgeBaseDocumentApiController {

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentApiController(KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade) {
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
    }

    @PostMapping("/{id}/documents")
    ResponseEntity<KnowledgeBaseDocument> uploadDocument(
        @PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
            knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
                id, file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream()));
    }

    @ExceptionHandler(KnowledgeBaseStorageLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleStorageLimitExceeded(KnowledgeBaseStorageLimitExceededException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONTENT_TOO_LARGE, exception.getMessage());

        return ResponseEntity.of(problemDetail)
            .build();
    }
}
