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

import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnWebhook
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

    @ExceptionHandler(BrowserVoiceSessionTokenService.VoiceUnavailableException.class)
    public ResponseEntity<String> handleVoiceUnavailable(
        BrowserVoiceSessionTokenService.VoiceUnavailableException voiceUnavailableException) {

        return ResponseEntity.status(501)
            .body(voiceUnavailableException.getMessage());
    }
}
