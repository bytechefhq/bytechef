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
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.facade.ApprovalInputException;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 * @deprecated This controller is deprecated and may be removed in a future version.
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
@Deprecated
public class ApprovalController {

    private static final String CONFIRMATION_PAGE = """
        <!doctype html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Confirm %1$s</title>
        </head>
        <body style="font-family: sans-serif; max-width: 32rem; margin: 4rem auto; padding: 0 1rem;">
        <h1>Confirm %1$s</h1>
        <p>Do you want to %2$s this request?</p>
        <form method="post"><button type="submit">%3$s</button></form>
        </body>
        </html>
        """;

    private static final String FORM_REQUIRED_MESSAGE =
        "This approval needs its form to be filled in. Open the approval form to answer it.";

    private static final String RESULT_PAGE = """
        <!doctype html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Approval</title>
        </head>
        <body style="font-family: sans-serif; max-width: 32rem; margin: 4rem auto; padding: 0 1rem;">
        <p>%s</p>
        </body>
        </html>
        """;

    private final ObjectProvider<ApprovalTokens> approvalTokensProvider;
    private final JobResumeFacade jobResumeFacade;

    @SuppressFBWarnings("EI")
    public ApprovalController(
        ObjectProvider<ApprovalTokens> approvalTokensProvider, JobResumeFacade jobResumeFacade) {

        this.approvalTokensProvider = approvalTokensProvider;
        this.jobResumeFacade = jobResumeFacade;
    }

    @SuppressFBWarnings(
        value = "VA_FORMAT_STRING_USES_NEWLINE",
        justification = "HTML page text block; the line separator inside the markup is irrelevant to rendering")
    @GetMapping(value = "/approvals/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@PathVariable String id) {
        Optional<ApprovalId> approvalIdOptional = parseApprovalId(id);

        if (approvalIdOptional.isEmpty()) {
            return htmlResponse(HttpStatus.BAD_REQUEST, "This approval link is not valid.");
        }

        ApprovalId approvalId = approvalIdOptional.get();

        String page = approvalId.isApproved()
            ? CONFIRMATION_PAGE.formatted("approval", "approve", "Approve")
            : CONFIRMATION_PAGE.formatted("rejection", "reject", "Reject");

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(page);
    }

    // CSRF-exempt: the path token is the capability, and the GET only renders this confirmation form
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @PostMapping(value = "/approvals/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approveFromConfirmationPage(@PathVariable String id) {
        Optional<ApprovalId> approvalIdOptional = parseApprovalId(id);

        if (approvalIdOptional.isEmpty()) {
            return htmlResponse(HttpStatus.BAD_REQUEST, "This approval link is not valid.");
        }

        JobResumeOutcome outcome;

        try {
            outcome = jobResumeFacade.resumeApproval(id);
        } catch (ApprovalInputException approvalInputException) {
            return htmlResponse(HttpStatus.BAD_REQUEST, FORM_REQUIRED_MESSAGE);
        }

        return htmlResponse(getStatus(outcome), getMessage(outcome));
    }

    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @PostMapping("/approvals/{id}")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        Optional<ApprovalId> approvalIdOptional = parseApprovalId(id);

        if (approvalIdOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        JobResumeOutcome outcome;

        try {
            outcome = jobResumeFacade.resumeApproval(id);
        } catch (ApprovalInputException approvalInputException) {
            return ResponseEntity.badRequest()
                .build();
        }

        return ResponseEntity.status(getStatus(outcome))
            .build();
    }

    private Optional<ApprovalId> parseApprovalId(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.empty();
        }

        Optional<String> innerTokenOptional = approvalTokens.parseSignedToken(id);

        if (innerTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ApprovalId.parse(innerTokenOptional.get()));
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return Optional.empty();
        }
    }

    private static HttpStatus getStatus(JobResumeOutcome outcome) {
        return switch (outcome) {
            case OK -> HttpStatus.NO_CONTENT;
            case INVALID_ID, STREAMING_NOT_ALLOWED -> HttpStatus.BAD_REQUEST;
            case GONE -> HttpStatus.GONE;
            case JOB_FAILED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case NOT_YET_SUSPENDED -> HttpStatus.CONFLICT;
        };
    }

    private static String getMessage(JobResumeOutcome outcome) {
        return switch (outcome) {
            case OK -> "Thank you, your response was recorded.";
            case INVALID_ID, STREAMING_NOT_ALLOWED -> "This approval link is not valid.";
            case GONE -> "This request is no longer available.";
            case JOB_FAILED -> "This request can no longer be answered because its workflow failed.";
            case NOT_YET_SUSPENDED -> "This request is still being prepared. Please try again in a moment.";
        };
    }

    @SuppressFBWarnings(
        value = "VA_FORMAT_STRING_USES_NEWLINE",
        justification = "HTML page text block; the line separator inside the markup is irrelevant to rendering")
    private static ResponseEntity<String> htmlResponse(HttpStatus status, String message) {
        HttpStatus pageStatus = status == HttpStatus.NO_CONTENT ? HttpStatus.OK : status;

        return ResponseEntity.status(pageStatus)
            .contentType(MediaType.TEXT_HTML)
            .body(RESULT_PAGE.formatted(message));
    }
}
