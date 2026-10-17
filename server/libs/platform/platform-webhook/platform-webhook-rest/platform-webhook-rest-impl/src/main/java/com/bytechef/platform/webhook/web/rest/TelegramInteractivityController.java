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
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
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
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TelegramInteractivityController {

    private final TelegramInteractivityHandler telegramInteractivityHandler;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public TelegramInteractivityController(
        ApprovalShortTokenService approvalShortTokenService, ConnectionService connectionService,
        JobResumeFacade jobResumeFacade, JobService jobService,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

        this.telegramInteractivityHandler = new TelegramInteractivityHandler(
            new ApprovalResolutionLookup(jobService, approvalTokens), approvalShortTokenService, connectionService,
            jobResumeFacade, approvalTokens, RestClient.create());
    }

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
            case UNAVAILABLE -> ResponseEntity.status(503)
                .build();
        };
    }
}
