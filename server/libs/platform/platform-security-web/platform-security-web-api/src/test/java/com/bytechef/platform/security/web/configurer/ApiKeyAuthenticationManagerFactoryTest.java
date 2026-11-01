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

package com.bytechef.platform.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class ApiKeyAuthenticationManagerFactoryTest {

    private ApplicationContext applicationContext;
    private AuthenticationEventPublisher authenticationEventPublisher;
    private AuthenticationProvider authenticationProvider;
    private HttpSecurity httpSecurity;
    private ObjectProvider<AuthenticationEventPublisher> authenticationEventPublisherProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        applicationContext = mock(ApplicationContext.class);
        authenticationEventPublisher = mock(AuthenticationEventPublisher.class);
        authenticationEventPublisherProvider = mock(ObjectProvider.class);
        authenticationProvider = mock(AuthenticationProvider.class);
        httpSecurity = mock(HttpSecurity.class);

        when(httpSecurity.getSharedObject(ApplicationContext.class)).thenReturn(applicationContext);
        when(applicationContext.getBeanProvider(AuthenticationEventPublisher.class))
            .thenReturn(authenticationEventPublisherProvider);
        when(authenticationProvider.supports(any())).thenReturn(true);
    }

    @Test
    void testSuccessPublishesTheCredentialEventInsteadOfTheLoginEvent() {
        when(authenticationEventPublisherProvider.getIfUnique()).thenReturn(authenticationEventPublisher);

        Authentication authentication = new TestingAuthenticationToken("user", null, List.of());

        authentication.setAuthenticated(true);

        when(authenticationProvider.authenticate(any())).thenReturn(authentication);

        AuthenticationManager authenticationManager =
            ApiKeyAuthenticationManagerFactory.create(httpSecurity, authenticationProvider);

        authenticationManager.authenticate(new TestingAuthenticationToken("secret", "secret"));

        ArgumentCaptor<ApplicationEvent> applicationEventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);

        verify(applicationContext).publishEvent(applicationEventCaptor.capture());

        assertThat(applicationEventCaptor.getValue()).isInstanceOf(CredentialAuthenticationSuccessEvent.class);

        verify(authenticationEventPublisher, never()).publishAuthenticationSuccess(any());
    }

    @Test
    void testEveryFailureIsDelegated() {
        when(authenticationEventPublisherProvider.getIfUnique()).thenReturn(authenticationEventPublisher);
        when(authenticationProvider.authenticate(any())).thenThrow(new BadCredentialsException("Invalid API key"));

        AuthenticationManager authenticationManager =
            ApiKeyAuthenticationManagerFactory.create(httpSecurity, authenticationProvider);

        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken("secret", "secret");

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> authenticationManager.authenticate(authenticationToken))
                .isInstanceOf(BadCredentialsException.class);
        }

        verify(authenticationEventPublisher, times(2)).publishAuthenticationFailure(any(), any());
        verify(applicationContext, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    void testSuccessIsRecordedWithoutAnAuthenticationEventPublisher() {
        Authentication authentication = new TestingAuthenticationToken("user", null, List.of());

        authentication.setAuthenticated(true);

        when(authenticationProvider.authenticate(any())).thenReturn(authentication);

        AuthenticationManager authenticationManager =
            ApiKeyAuthenticationManagerFactory.create(httpSecurity, authenticationProvider);

        authenticationManager.authenticate(new TestingAuthenticationToken("secret", "secret"));

        verify(applicationContext).publishEvent(any(CredentialAuthenticationSuccessEvent.class));
    }
}
