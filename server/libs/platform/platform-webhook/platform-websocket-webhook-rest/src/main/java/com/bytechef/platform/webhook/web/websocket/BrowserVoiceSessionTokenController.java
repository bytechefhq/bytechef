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

package com.bytechef.platform.webhook.web.websocket;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues short-lived single-use tokens that authorize opening a browser-voice WebSocket session for a published
 * workflow webhook. Lives under {@code /webhooks/**} which is permit-all (same security model as inbound webhook
 * delivery); the real security comes from
 *
 * <ul>
 * <li>the single-use token semantics enforced by {@link BrowserVoiceSessionTokenService},</li>
 * <li>the voice-capability check that rejects mints for non-voice or non-existent webhooks at issue time,</li>
 * <li>any webhook-signature configuration on the trigger itself.</li>
 * </ul>
 *
 * <p>
 * CORS is opened to all origins because customer sites embedding the chat widget will commonly be a different origin
 * than the ByteChef host serving the webhook.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/webhooks/{webhookId}")
@CrossOrigin(originPatterns = "*")
public class BrowserVoiceSessionTokenController {

    private final BrowserVoiceSessionTokenService tokenService;

    public BrowserVoiceSessionTokenController(BrowserVoiceSessionTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/voice-session-token")
    public BrowserVoiceSessionTokenService.Token issueToken(@PathVariable String webhookId) {
        return tokenService.issue(webhookId);
    }

    @ExceptionHandler(BrowserVoiceSessionTokenService.InvalidWebhookException.class)
    public ResponseEntity<String> handleInvalidWebhook(BrowserVoiceSessionTokenService.InvalidWebhookException ex) {
        return ResponseEntity.status(404)
            .body(ex.getMessage());
    }
}
