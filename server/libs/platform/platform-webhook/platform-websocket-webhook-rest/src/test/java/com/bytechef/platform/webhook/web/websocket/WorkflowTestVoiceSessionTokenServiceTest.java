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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowTestVoiceSessionTokenServiceTest {

    private final WorkflowTestVoiceSessionTokenService tokenService = new WorkflowTestVoiceSessionTokenService();

    @Test
    void testConsumeReturnsTheEnvironmentTheTokenWasMintedFor() {
        WorkflowTestVoiceSessionTokenService.Token token = tokenService.issue("wf-1", 3L);

        assertThat(tokenService.consume(token.token(), "wf-1")).hasValue(3L);
    }

    @Test
    void testTokenIsSingleUse() {
        WorkflowTestVoiceSessionTokenService.Token token = tokenService.issue("wf-1", 3L);

        assertThat(tokenService.consume(token.token(), "wf-1")).hasValue(3L);
        assertThat(tokenService.consume(token.token(), "wf-1")).isEmpty();
    }

    @Test
    void testTokenForAnotherWorkflowIsRejected() {
        WorkflowTestVoiceSessionTokenService.Token token = tokenService.issue("wf-1", 3L);

        assertThat(tokenService.consume(token.token(), "wf-2")).isEmpty();
    }
}
