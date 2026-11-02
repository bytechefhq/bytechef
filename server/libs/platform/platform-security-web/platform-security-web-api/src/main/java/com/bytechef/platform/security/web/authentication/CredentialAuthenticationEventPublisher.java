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

import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public final class CredentialAuthenticationEventPublisher implements AuthenticationEventPublisher {

    static final Duration FAILURE_LOG_INTERVAL = Duration.ofMinutes(1);

    private static final Logger log = LoggerFactory.getLogger(CredentialAuthenticationEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;
    private final FailureLogRateLimiter failureLogRateLimiter;

    public CredentialAuthenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

        this(applicationEventPublisher, authenticationEventPublisher, Clock.systemUTC());
    }

    @SuppressFBWarnings("EI")
    CredentialAuthenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher, Clock clock) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.authenticationEventPublisher = authenticationEventPublisher;
        this.failureLogRateLimiter = new FailureLogRateLimiter(clock, FAILURE_LOG_INTERVAL);
    }

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        if (authentication instanceof McpAnonymousAuthenticationToken) {
            return;
        }

        try {
            applicationEventPublisher.publishEvent(new CredentialAuthenticationSuccessEvent(authentication));
        } catch (RuntimeException exception) {
            OptionalLong suppressedFailures = failureLogRateLimiter.tryAcquire();

            if (suppressedFailures.isPresent()) {
                log.error(
                    "AUDIT LISTENER FAILURE: a listener of the CREDENTIAL_USED event ({}) failed, so the entry may " +
                        "not have been recorded; the request is unaffected; {} similar failures suppressed since the " +
                        "last report",
                    getAuthenticationType(authentication), suppressedFailures.getAsLong(), exception);
            }
        }
    }

    @Override
    public void publishAuthenticationFailure(
        AuthenticationException authenticationException, Authentication authentication) {

        if (authenticationEventPublisher == null ||
            authenticationException instanceof McpServerDisabledException) {

            return;
        }

        try {
            authenticationEventPublisher.publishAuthenticationFailure(authenticationException, authentication);
        } catch (RuntimeException exception) {
            OptionalLong suppressedFailures = failureLogRateLimiter.tryAcquire();

            if (suppressedFailures.isPresent()) {
                log.error(
                    "AUDIT LISTENER FAILURE: a listener of the AUTHENTICATION_FAILURE event ({}, {}) failed, so the " +
                        "entry may not have been recorded; {} similar failures suppressed since the last report",
                    getAuthenticationType(authentication), authenticationException.getMessage(),
                    suppressedFailures.getAsLong(), exception);
            }
        }
    }

    private static String getAuthenticationType(Authentication authentication) {
        Class<? extends Authentication> authenticationClass = authentication.getClass();

        return authenticationClass.getSimpleName();
    }
}
