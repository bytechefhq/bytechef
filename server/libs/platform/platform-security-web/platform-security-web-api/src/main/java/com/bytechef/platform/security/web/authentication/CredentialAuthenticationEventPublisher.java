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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public final class CredentialAuthenticationEventPublisher implements AuthenticationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;

    @SuppressFBWarnings("EI")
    public CredentialAuthenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.authenticationEventPublisher = authenticationEventPublisher;
    }

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        applicationEventPublisher.publishEvent(new CredentialAuthenticationSuccessEvent(authentication));
    }

    @Override
    public void publishAuthenticationFailure(
        AuthenticationException authenticationException, Authentication authentication) {

        if (authenticationEventPublisher != null) {
            authenticationEventPublisher.publishAuthenticationFailure(authenticationException, authentication);
        }
    }
}
