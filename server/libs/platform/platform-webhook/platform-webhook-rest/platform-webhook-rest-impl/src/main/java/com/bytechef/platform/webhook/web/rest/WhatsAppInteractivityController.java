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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class WhatsAppInteractivityController {

    private final WhatsAppInteractivityHandler whatsAppInteractivityHandler;
    private final @Nullable String verifyToken;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WhatsAppInteractivityController(
        ApplicationProperties applicationProperties, ConnectionService connectionService,
        JobResumeFacade jobResumeFacade, JobService jobService,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        ApplicationProperties.Webhook webhook = applicationProperties.getWebhook();

        ApplicationProperties.Webhook.Whatsapp whatsapp = webhook.getWhatsapp();

        this.verifyToken = whatsapp.getVerifyToken();
        ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

        this.whatsAppInteractivityHandler = new WhatsAppInteractivityHandler(
            new ApprovalResolutionLookup(jobService, approvalTokens), connectionService, jobResumeFacade,
            approvalTokens, RestClient.create());
    }

    @GetMapping(value = "/whatsapp/interactivity")
    public ResponseEntity<String> verify(
        @RequestParam(value = "hub.mode", required = false) String mode,
        @RequestParam(value = "hub.verify_token", required = false) String token,
        @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && verifyToken != null && !verifyToken.isBlank() && token != null
            && MessageDigest.isEqual(
                verifyToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403)
            .build();
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Meta posts here directly, authenticated by the request signature")
    @PostMapping(value = "/whatsapp/interactivity", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleInteractivity(
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        @RequestBody String rawBody) {

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(rawBody, signature);

        return switch (result) {
            case HANDLED, IGNORED -> ResponseEntity.ok()
                .build();
            case UNAUTHORIZED -> ResponseEntity.status(401)
                .build();
            case UNAVAILABLE -> ResponseEntity.status(503)
                .build();
        };
    }
}
