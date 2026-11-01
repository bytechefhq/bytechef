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

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
class CredentialAuthenticationEventPublisherTest {

    private static final Authentication AUTHENTICATION =
        UsernamePasswordAuthenticationToken.authenticated("user", null, List.of());

    @Test
    void testFailingSuccessListenerDoesNotFailTheRequest() {
        CredentialAuthenticationEventPublisher credentialAuthenticationEventPublisher =
            new CredentialAuthenticationEventPublisher(
                event -> {
                    throw new IllegalStateException("audit store unavailable");
                }, null);

        assertThatCode(() -> credentialAuthenticationEventPublisher.publishAuthenticationSuccess(AUTHENTICATION))
            .doesNotThrowAnyException();
    }

    @Test
    void testSuccessIsPublishedAsACredentialEvent() {
        List<Object> events = new ArrayList<>();

        CredentialAuthenticationEventPublisher credentialAuthenticationEventPublisher =
            new CredentialAuthenticationEventPublisher(events::add, null);

        credentialAuthenticationEventPublisher.publishAuthenticationSuccess(AUTHENTICATION);

        assertThat(events)
            .singleElement()
            .isInstanceOf(CredentialAuthenticationSuccessEvent.class);
    }

    @Test
    void testAnonymousMcpAccessIsNotPublishedAsACredentialUse() {
        List<Object> events = new ArrayList<>();

        CredentialAuthenticationEventPublisher credentialAuthenticationEventPublisher =
            new CredentialAuthenticationEventPublisher(events::add, null);

        credentialAuthenticationEventPublisher.publishAuthenticationSuccess(
            McpAnonymousAuthenticationToken.of("anonymous-mcp-server-1"));

        assertThat(events).isEmpty();
    }

    @Test
    void testFailureIsDelegated() {
        RecordingAuthenticationEventPublisher recordingAuthenticationEventPublisher =
            new RecordingAuthenticationEventPublisher();

        CredentialAuthenticationEventPublisher credentialAuthenticationEventPublisher =
            new CredentialAuthenticationEventPublisher(event -> {}, recordingAuthenticationEventPublisher);

        BadCredentialsException badCredentialsException = new BadCredentialsException("Invalid API key");

        credentialAuthenticationEventPublisher.publishAuthenticationFailure(badCredentialsException, AUTHENTICATION);

        assertThat(recordingAuthenticationEventPublisher.failures).containsExactly(badCredentialsException);
    }

    @Test
    void testDisabledMcpServerIsNotRecordedAsAFailure() {
        RecordingAuthenticationEventPublisher recordingAuthenticationEventPublisher =
            new RecordingAuthenticationEventPublisher();

        CredentialAuthenticationEventPublisher credentialAuthenticationEventPublisher =
            new CredentialAuthenticationEventPublisher(event -> {}, recordingAuthenticationEventPublisher);

        credentialAuthenticationEventPublisher.publishAuthenticationFailure(
            new McpServerDisabledException(McpServerDisabledException.MCP_SERVER_MESSAGE), AUTHENTICATION);

        assertThat(recordingAuthenticationEventPublisher.failures).isEmpty();
    }

    private static final class RecordingAuthenticationEventPublisher implements AuthenticationEventPublisher {

        private final List<AuthenticationException> failures = new ArrayList<>();

        @Override
        public void publishAuthenticationSuccess(Authentication authentication) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publishAuthenticationFailure(
            AuthenticationException authenticationException, Authentication authentication) {

            failures.add(authenticationException);
        }
    }
}
