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
 * Public endpoint for Slack interactivity callbacks resolving approvals in place — the Slack app's Interactivity
 * Request URL points here. Anonymous like the job-resume endpoint it delegates to; authenticity comes from the
 * {@code X-Slack-Signature} HMAC verified against the signing secret stored on the Slack connection (see
 * {@link SlackInteractivityHandler}), so unverifiable callbacks are rejected without acting.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class SlackInteractivityController {

    private final SlackInteractivityHandler slackInteractivityHandler;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public SlackInteractivityController(
        ConnectionService connectionService, JobResumeFacade jobResumeFacade,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        this.slackInteractivityHandler = new SlackInteractivityHandler(
            connectionService, jobResumeFacade, approvalTokensObjectProvider.getIfAvailable(), RestClient.create());
    }

    /**
     * Slack retries interactivity callbacks that do not answer 200 within 3 seconds, so ignorable payloads (other
     * action ids, non-block_actions types) still answer 200; only a failed signature check answers 401.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Slack posts here directly, authenticated by the request signature")
    @PostMapping(value = "/slack/interactivity", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleInteractivity(
        @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
        @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
        @RequestBody String rawBody) {

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(rawBody, timestamp, signature);

        return switch (result) {
            case HANDLED, IGNORED -> ResponseEntity.ok()
                .build();
            case UNAUTHORIZED -> ResponseEntity.status(401)
                .build();
        };
    }
}
