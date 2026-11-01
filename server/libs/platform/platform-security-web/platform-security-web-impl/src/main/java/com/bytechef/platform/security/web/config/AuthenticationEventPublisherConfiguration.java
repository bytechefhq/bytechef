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

import com.bytechef.platform.security.exception.UserNotActivatedException;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AuthenticationEventPublisherConfiguration {

    @Bean
    DefaultAuthenticationEventPublisher authenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher) {

        DefaultAuthenticationEventPublisher authenticationEventPublisher =
            new DefaultAuthenticationEventPublisher(applicationEventPublisher);

        Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings =
            Map.of(
                InternalAuthenticationServiceException.class, AuthenticationFailureServiceExceptionEvent.class,
                UserNotActivatedException.class, AuthenticationFailureDisabledEvent.class);

        authenticationEventPublisher.setAdditionalExceptionMappings(mappings);
        authenticationEventPublisher.setDefaultAuthenticationFailureEvent(
            AuthenticationFailureBadCredentialsEvent.class);

        return authenticationEventPublisher;
    }
}
