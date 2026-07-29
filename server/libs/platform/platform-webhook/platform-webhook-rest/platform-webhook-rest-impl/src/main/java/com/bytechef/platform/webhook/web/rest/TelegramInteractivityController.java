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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Public endpoint for Telegram {@code callback_query} webhooks resolving approvals in place — the bot's webhook (set
 * via {@code setWebhook} with a secret token) points here. Anonymous like the job-resume endpoint the handler delegates
 * to; authenticity comes from the {@code X-Telegram-Bot-Api-Secret-Token} header verified against the secret token
 * stored on the Telegram connection (see {@link TelegramInteractivityHandler}). Because a Telegram bot has a single
 * webhook, use a dedicated approvals bot so this does not collide with a Telegram trigger.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class TelegramInteractivityController {

    private final TelegramInteractivityHandler telegramInteractivityHandler;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public TelegramInteractivityController(
        ApprovalShortTokenStore approvalShortTokenStore, ConnectionService connectionService,
        JobResumeFacade jobResumeFacade, ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        this.telegramInteractivityHandler = new TelegramInteractivityHandler(
            approvalShortTokenStore, connectionService, jobResumeFacade,
            approvalTokensObjectProvider.getIfAvailable(), RestClient.create());
    }

    /**
     * Telegram retries webhook deliveries that do not answer 200, so ignorable payloads (non-callback updates, unknown
     * short ids) still answer 200; only a failed secret-token check answers 401.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Telegram posts here directly, authenticated by the secret-token header")
    @PostMapping(value = "/telegram/interactivity", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleInteractivity(
        @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
        @RequestBody String rawBody) {

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(rawBody, secretToken);

        return switch (result) {
            case HANDLED, IGNORED -> ResponseEntity.ok()
                .build();
            case UNAUTHORIZED -> ResponseEntity.status(401)
                .build();
        };
    }
}
