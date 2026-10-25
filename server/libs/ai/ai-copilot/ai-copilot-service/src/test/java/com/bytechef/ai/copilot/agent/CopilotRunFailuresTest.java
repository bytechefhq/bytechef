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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

/**
 * @author Ivica Cardic
 */
class CopilotRunFailuresTest {

    @Test
    void testDescribesAMissingProvider() {
        assertThat(CopilotRunFailures.describe(new IllegalStateException(
            "No chat provider is activated for environment PRODUCTION. Activate the Anthropic or OpenAI provider " +
                "(or set bytechef.ai.provider.anthropic.api-key)")))
                    .isEqualTo(CopilotRunFailures.NO_PROVIDER);
    }

    @Test
    void testDescribesAnAccountOutOfCredits() {
        RuntimeException anthropicCredits = new RuntimeException(
            "400: {\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Your credit " +
                "balance is too low to access the Anthropic API.\"}}");

        assertThat(CopilotRunFailures.describe(new IllegalStateException("Chat stream failed", anthropicCredits)))
            .isEqualTo(CopilotRunFailures.OUT_OF_CREDITS);
        assertThat(CopilotRunFailures.describe(new RuntimeException(
            "HTTP 429 - {\"error\":{\"message\":\"You exceeded your current quota\",\"type\":\"insufficient_quota\"}}")))
                .isEqualTo(CopilotRunFailures.OUT_OF_CREDITS);
    }

    @Test
    void testDescribesARateLimitFoundInTheCauseChain() {
        RuntimeException rateLimit = new RuntimeException(
            "HTTP 429: {\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\"}}");

        assertThat(CopilotRunFailures.describe(new IllegalStateException("Chat stream failed", rateLimit)))
            .isEqualTo(CopilotRunFailures.RATE_LIMITED);
    }

    @Test
    void testDescribesAnOverloadedProvider() {
        assertThat(CopilotRunFailures.describe(new RuntimeException("529 Overloaded")))
            .isEqualTo(CopilotRunFailures.OVERLOADED);
    }

    @Test
    void testDescribesARejectedApiKey() {
        assertThat(CopilotRunFailures.describe(new RuntimeException("Incorrect API key provided: sk-proj-1234")))
            .isEqualTo(CopilotRunFailures.API_KEY_REJECTED);
        assertThat(CopilotRunFailures.describe(new RuntimeException("401 {\"message\":\"invalid x-api-key\"}")))
            .isEqualTo(CopilotRunFailures.API_KEY_REJECTED);
    }

    @Test
    void testDescribesAConversationTooLongForTheModel() {
        assertThat(CopilotRunFailures.describe(new RuntimeException("prompt is too long: 210000 tokens > 200000")))
            .isEqualTo(CopilotRunFailures.CONVERSATION_TOO_LONG);
    }

    @Test
    void testLeavesOtherFailuresUndescribed() {
        assertThat(CopilotRunFailures.describe(
            new IllegalStateException("relation \"workflow\" does not exist at character 4290")))
                .isNull();
        assertThat(CopilotRunFailures.describe(new NullPointerException())).isNull();
    }

    @Test
    void testLeavesASignedOutCallerUndescribed() {
        assertThat(CopilotRunFailures.describe(
            new AuthenticationCredentialsNotFoundException("An Authentication object was not found"))).isNull();
        assertThat(CopilotRunFailures.describe(
            new IllegalStateException("Chat stream failed", new InsufficientAuthenticationException("401")))).isNull();
    }

    @Test
    void testLeavesAMissingApiKeyUndescribed() {
        assertThat(CopilotRunFailures.describe(new IllegalStateException("No API key configured for OpenAI")))
            .isNull();
        assertThat(CopilotRunFailures.describe(new IllegalArgumentException("Anthropic API key must be set")))
            .isNull();
    }

    @Test
    void testLeavesStatusLikeNumbersOutsideAnHttpStatusUndescribed() {
        assertThat(CopilotRunFailures.describe(new IllegalStateException("Workflow 401 has 429 tasks"))).isNull();
    }

    @Test
    void testDescribesStatusCodesInTheirUsualPositions() {
        assertThat(CopilotRunFailures.describe(new RuntimeException("429 Too Many Requests from POST https://x")))
            .isEqualTo(CopilotRunFailures.RATE_LIMITED);
        assertThat(CopilotRunFailures.describe(new RuntimeException("Request failed with status code: 401")))
            .isEqualTo(CopilotRunFailures.API_KEY_REJECTED);
    }

    @Test
    void testStopsOnACyclicCauseChain() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);

        first.initCause(second);

        assertThat(CopilotRunFailures.describe(first)).isNull();
    }
}
