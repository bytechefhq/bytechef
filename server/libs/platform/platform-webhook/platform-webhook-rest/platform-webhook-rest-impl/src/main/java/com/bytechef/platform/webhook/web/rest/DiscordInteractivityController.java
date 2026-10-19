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
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class DiscordInteractivityController {

    private final DiscordInteractivityHandler discordInteractivityHandler;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public DiscordInteractivityController(
        ApplicationProperties applicationProperties, ApprovalShortTokenService approvalShortTokenService,
        JobResumeFacade jobResumeFacade, JobService jobService,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        ApplicationProperties.Webhook webhook = applicationProperties.getWebhook();

        ApplicationProperties.Webhook.Discord discord = webhook.getDiscord();

        ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

        this.discordInteractivityHandler = new DiscordInteractivityHandler(
            new ApprovalResolutionLookup(jobService, approvalTokens), approvalShortTokenService, jobResumeFacade,
            approvalTokens, discord.getPublicKey());
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
