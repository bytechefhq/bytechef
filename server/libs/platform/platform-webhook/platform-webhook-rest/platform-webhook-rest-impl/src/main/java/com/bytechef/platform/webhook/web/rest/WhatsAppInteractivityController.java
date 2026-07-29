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
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for WhatsApp Cloud API webhooks resolving approvals in place — the Meta app's webhook Callback URL
 * points here. Anonymous like the job-resume endpoint the handler delegates to; authenticity comes from the
 * {@code X-Hub-Signature-256} HMAC verified against the app secret stored on the WhatsApp connection (see
 * {@link WhatsAppInteractivityHandler}). The GET method answers Meta's one-time verification handshake against the
 * configured {@code bytechef.webhook.whatsapp.verify-token}.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class WhatsAppInteractivityController {

    private final WhatsAppInteractivityHandler whatsAppInteractivityHandler;
    private final @Nullable String verifyToken;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WhatsAppInteractivityController(
        ConnectionService connectionService, JobResumeFacade jobResumeFacade,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        @Value("${bytechef.webhook.whatsapp.verify-token:}") String verifyToken) {

        this.whatsAppInteractivityHandler = new WhatsAppInteractivityHandler(
            connectionService, jobResumeFacade, approvalTokensObjectProvider.getIfAvailable());
        this.verifyToken = verifyToken;
    }

    /**
     * Meta's webhook verification handshake: echoes {@code hub.challenge} only when {@code hub.mode=subscribe} and
     * {@code hub.verify_token} matches the configured token (constant-time). Returns 403 otherwise so an endpoint with
     * no configured token cannot be registered.
     */
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

    /**
     * Meta retries webhook deliveries that do not answer 200 quickly, so ignorable payloads (status callbacks,
     * non-button messages) still answer 200; only a failed signature check answers 401.
     */
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
        };
    }
}
