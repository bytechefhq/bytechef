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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues short-lived single-use tokens that authorize opening a workflow-test voice WebSocket. Gated by the standard
 * {@code /internal/**} session cookie. Coordinator-only — matches the sibling {@code WorkflowTestApiController} guard
 * so the endpoint does not register in worker / scheduler app deployments.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal/workflow-tests")
@ConditionalOnCoordinator
public class WorkflowTestVoiceSessionTokenController {

    private final WorkflowTestVoiceSessionTokenService tokenService;

    public WorkflowTestVoiceSessionTokenController(WorkflowTestVoiceSessionTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/{workflowId}/voice-session-token")
    public WorkflowTestVoiceSessionTokenService.Token issueToken(@PathVariable String workflowId) {
        return tokenService.issue(workflowId);
    }
}
