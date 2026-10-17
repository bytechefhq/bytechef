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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class SlackInteractivityController {

    private final SlackInteractivityHandler slackInteractivityHandler;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public SlackInteractivityController(
        ConnectionService connectionService, JobResumeFacade jobResumeFacade, JobService jobService,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

        this.slackInteractivityHandler = new SlackInteractivityHandler(
            new ApprovalResolutionLookup(jobService, approvalTokens), connectionService, jobResumeFacade,
            approvalTokens, RestClient.create());
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Slack posts here directly, authenticated by the request signature")
    @PostMapping(value = "/slack/interactivity", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleInteractivity(
        @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
        @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
        HttpServletRequest httpServletRequest) throws IOException {

        String rawBody = readRawBody(httpServletRequest);

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(rawBody, timestamp, signature);

        return switch (result) {
            case HANDLED, IGNORED -> ResponseEntity.ok()
                .build();
            case UNAUTHORIZED -> ResponseEntity.status(401)
                .build();
            case UNAVAILABLE -> ResponseEntity.status(503)
                .build();
        };
    }

    private static String readRawBody(HttpServletRequest httpServletRequest) throws IOException {
        try (InputStream inputStream = httpServletRequest.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();

            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
