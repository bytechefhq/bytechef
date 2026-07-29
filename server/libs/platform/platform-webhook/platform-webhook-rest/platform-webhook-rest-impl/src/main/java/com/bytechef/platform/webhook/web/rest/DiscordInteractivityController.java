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
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for Discord interaction webhooks resolving approvals in place — the Discord app's Interactions
 * Endpoint URL points here. Anonymous like the job-resume endpoint the handler delegates to; authenticity comes from
 * the Ed25519 request signature verified against {@code bytechef.webhook.discord.public-key} (see
 * {@link DiscordInteractivityHandler}). The interaction response is returned inline as the HTTP body (PONG for the
 * registration handshake, an UPDATE_MESSAGE for a resolved button).
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class DiscordInteractivityController {

    private final DiscordInteractivityHandler discordInteractivityHandler;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public DiscordInteractivityController(
        ApprovalShortTokenStore approvalShortTokenStore, JobResumeFacade jobResumeFacade,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        @Value("${bytechef.webhook.discord.public-key:}") String publicKey) {

        this.discordInteractivityHandler = new DiscordInteractivityHandler(
            approvalShortTokenStore, jobResumeFacade, approvalTokensObjectProvider.getIfAvailable(), publicKey);
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Discord posts here directly, authenticated by the Ed25519 signature")
    @PostMapping(
        value = "/discord/interactivity", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleInteractivity(
        @RequestHeader(value = "X-Signature-Ed25519", required = false) String signature,
        @RequestHeader(value = "X-Signature-Timestamp", required = false) String timestamp,
        @RequestBody String rawBody) {

        DiscordInteractivityHandler.Response response = discordInteractivityHandler.handle(
            rawBody, signature, timestamp);

        return ResponseEntity.status(response.statusCode())
            .body(response.body());
    }
}
