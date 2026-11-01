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

package com.bytechef.platform.security.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.exception.UserNotActivatedException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
class AuthenticationEventPublisherConfigurationTest {

    private static final Authentication AUTHENTICATION =
        UsernamePasswordAuthenticationToken.unauthenticated("user", null);

    private final List<Object> events = new ArrayList<>();

    private DefaultAuthenticationEventPublisher authenticationEventPublisher;

    @BeforeEach
    void beforeEach() {
        AuthenticationEventPublisherConfiguration authenticationEventPublisherConfiguration =
            new AuthenticationEventPublisherConfiguration();

        authenticationEventPublisher = authenticationEventPublisherConfiguration.authenticationEventPublisher(
            events::add);
    }

    @Test
    void testInactiveUserIsRecordedAsDisabled() {
        authenticationEventPublisher.publishAuthenticationFailure(
            new UserNotActivatedException("User admin@localhost.com was not activated"), AUTHENTICATION);

        assertThat(events)
            .singleElement()
            .isInstanceOf(AuthenticationFailureDisabledEvent.class);
    }

    @Test
    void testInternalServiceFailureIsRecordedAsAServiceFailure() {
        authenticationEventPublisher.publishAuthenticationFailure(
            new InternalAuthenticationServiceException("Database unavailable"), AUTHENTICATION);

        assertThat(events)
            .singleElement()
            .isInstanceOf(AuthenticationFailureServiceExceptionEvent.class);
    }

    @Test
    void testUnmappedExceptionIsRecordedAsBadCredentials() {
        authenticationEventPublisher.publishAuthenticationFailure(
            new UnmappedAuthenticationException("Admin API key required"), AUTHENTICATION);

        assertThat(events)
            .singleElement()
            .isInstanceOf(AuthenticationFailureBadCredentialsEvent.class);
    }

    @Test
    void testBuiltInMappingsAreKept() {
        authenticationEventPublisher.publishAuthenticationFailure(
            new BadCredentialsException("Invalid API key"), AUTHENTICATION);
        authenticationEventPublisher.publishAuthenticationFailure(
            new AuthenticationServiceException("MCP server secret verification failed"), AUTHENTICATION);

        assertThat(events)
            .hasSize(2)
            .hasExactlyElementsOfTypes(
                AuthenticationFailureBadCredentialsEvent.class, AuthenticationFailureServiceExceptionEvent.class);
    }

    private static final class UnmappedAuthenticationException extends AuthenticationException {

        private UnmappedAuthenticationException(String message) {
            super(message);
        }
    }
}
