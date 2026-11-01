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

package com.bytechef.platform.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.authentication.UnresolvedCredentialAuthenticationToken;
import com.bytechef.platform.security.web.configurer.ApiKeyAuthenticationManagerFactory;
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * @author Ivica Cardic
 */
class ApiKeyAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private ApiKeyAuthenticationFilter filter;
    private MockFilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void beforeEach() {
        authenticationManager = mock(AuthenticationManager.class);

        filter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> new TestApiKeyAuthenticationToken(), authenticationManager);
        filterChain = new MockFilterChain();
        request = new MockHttpServletRequest("POST", "/api/management/secret/mcp");
        response = new MockHttpServletResponse();
    }

    @Test
    void testDoFilterRespondsUnauthorizedWhenAuthenticationFails() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid secret key"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void testDoFilterRespondsForbiddenWithServerDisabledBodyWhenTheServerIsDisabled() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new McpServerDisabledException(McpServerDisabledException.MCP_SERVER_MESSAGE));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("\"error\":\"server_disabled\"")
            .contains(McpServerDisabledException.MCP_SERVER_MESSAGE);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void testDoFilterContinuesChainWhenAuthenticationSucceeds() throws Exception {
        TestApiKeyAuthenticationToken authenticatedToken = new TestApiKeyAuthenticationToken();

        authenticatedToken.setAuthenticated(true);

        when(authenticationManager.authenticate(any())).thenReturn(authenticatedToken);

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void testNonAuthenticationFailureRespondsServiceUnavailableAndIsPublished() throws Exception {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        IllegalStateException illegalStateException = new IllegalStateException("Database is down");

        when(authenticationManager.authenticate(any())).thenThrow(illegalStateException);

        ApiKeyAuthenticationFilter publishingFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> new TestApiKeyAuthenticationToken(), authenticationManager,
            authenticationEventPublisher);

        publishingFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor =
            ArgumentCaptor.forClass(AuthenticationException.class);

        verify(authenticationEventPublisher).publishAuthenticationFailure(
            authenticationExceptionCaptor.capture(), any(TestApiKeyAuthenticationToken.class));

        assertThat(authenticationExceptionCaptor.getValue())
            .isInstanceOf(AuthenticationServiceException.class)
            .hasCause(illegalStateException);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void testNonAuthenticationFailureIsLoggedWithoutTheRequestPath() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new IllegalStateException("Database unavailable"));

        Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            filter.doFilter(request, response, filterChain);
        } finally {
            logger.detachAppender(listAppender);
        }

        String servletPath = request.getRequestURI();

        assertThat(listAppender.list)
            .isNotEmpty()
            .extracting(ILoggingEvent::getFormattedMessage)
            .noneMatch(formattedMessage -> formattedMessage.contains(servletPath));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void testAuthenticationFailureFurtherDownTheChainOfAnAuthenticatedRequestIsUnauthorizedAndNotPublished()
        throws Exception {

        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        TestApiKeyAuthenticationToken authenticatedToken = new TestApiKeyAuthenticationToken();

        authenticatedToken.setAuthenticated(true);

        when(authenticationManager.authenticate(any())).thenReturn(authenticatedToken);

        ApiKeyAuthenticationFilter publishingFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> new TestApiKeyAuthenticationToken(), authenticationManager,
            authenticationEventPublisher);

        publishingFilter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new BadCredentialsException("Rejected by a later filter");
        });

        verify(authenticationEventPublisher, never()).publishAuthenticationFailure(any(), any());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testFailureFurtherDownTheChainIsNotTreatedAsAnAuthenticationFailure() {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        TestApiKeyAuthenticationToken authenticatedToken = new TestApiKeyAuthenticationToken();

        authenticatedToken.setAuthenticated(true);

        when(authenticationManager.authenticate(any())).thenReturn(authenticatedToken);

        ApiKeyAuthenticationFilter publishingFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> new TestApiKeyAuthenticationToken(), authenticationManager,
            authenticationEventPublisher);

        assertThatThrownBy(() -> publishingFilter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new IllegalStateException("Controller failed");
        }))
            .hasRootCauseInstanceOf(IllegalStateException.class);

        verify(authenticationEventPublisher, never()).publishAuthenticationFailure(any(), any());
    }

    @Test
    void testRejectedCredentialIsPublishedAsAFailure() throws Exception {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        BadCredentialsException badCredentialsException = new BadCredentialsException("Invalid signature");

        ApiKeyAuthenticationFilter rejectingFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> {
                throw badCredentialsException;
            }, authenticationManager, authenticationEventPublisher);

        rejectingFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationEventPublisher).publishAuthenticationFailure(
            eq(badCredentialsException), authenticationCaptor.capture());
        verify(authenticationManager, never()).authenticate(any());

        Authentication authentication = authenticationCaptor.getValue();

        assertThat(authentication.getName()).isEqualTo(UnresolvedCredentialAuthenticationToken.PRINCIPAL);
        assertThat(authentication.getDetails()).isInstanceOfSatisfying(
            WebAuthenticationDetails.class,
            webAuthenticationDetails -> assertThat(webAuthenticationDetails.getRemoteAddress())
                .isEqualTo(request.getRemoteAddr()));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testFailedLookupDuringConversionIsPublishedAndRespondsServiceUnavailable() throws Exception {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        IllegalStateException illegalStateException = new IllegalStateException("Signing key lookup failed");

        ApiKeyAuthenticationFilter failingFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> {
                throw illegalStateException;
            }, authenticationManager, authenticationEventPublisher);

        failingFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor =
            ArgumentCaptor.forClass(AuthenticationException.class);
        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationEventPublisher).publishAuthenticationFailure(
            authenticationExceptionCaptor.capture(), authenticationCaptor.capture());
        verify(authenticationManager, never()).authenticate(any());

        Authentication authentication = authenticationCaptor.getValue();

        assertThat(authenticationExceptionCaptor.getValue())
            .isInstanceOf(AuthenticationServiceException.class)
            .hasCause(illegalStateException);
        assertThat(authentication.getName()).isEqualTo(UnresolvedCredentialAuthenticationToken.PRINCIPAL);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void testCredentialRejectedByTheManagerIsPublishedOnce() throws Exception {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);

        when(authenticationProvider.supports(any())).thenReturn(true);
        when(authenticationProvider.authenticate(any())).thenThrow(new BadCredentialsException("Unknown key"));

        ApiKeyAuthenticationFilter managerFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> new TestApiKeyAuthenticationToken(),
            ApiKeyAuthenticationManagerFactory.create(authenticationProvider, authenticationEventPublisher),
            authenticationEventPublisher);

        managerFilter.doFilter(request, response, filterChain);

        verify(authenticationEventPublisher, times(1)).publishAuthenticationFailure(
            any(AuthenticationException.class), any(Authentication.class));
        verify(authenticationEventPublisher, never()).publishAuthenticationFailure(
            any(AuthenticationException.class), any(UnresolvedCredentialAuthenticationToken.class));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testCredentialRejectedByTheManagerCarriesTheRequestDetails() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Unknown key"));

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        Authentication authentication = authenticationCaptor.getValue();

        assertThat(authentication.getDetails()).isInstanceOfSatisfying(
            TenantWebAuthenticationDetails.class,
            tenantWebAuthenticationDetails -> {
                assertThat(tenantWebAuthenticationDetails.getRemoteAddress()).isEqualTo(request.getRemoteAddr());
                assertThat(tenantWebAuthenticationDetails.getTenantId()).isEqualTo("public");
            });
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testAuthenticationFailureFurtherDownTheChainIsNotPublished() throws Exception {
        AuthenticationEventPublisher authenticationEventPublisher = mock(AuthenticationEventPublisher.class);

        ApiKeyAuthenticationFilter noKeyFilter = new ApiKeyAuthenticationFilter(
            servletRequest -> true, servletRequest -> null, authenticationManager, authenticationEventPublisher);

        noKeyFilter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new BadCredentialsException("Rejected by a later filter");
        });

        verify(authenticationEventPublisher, never()).publishAuthenticationFailure(any(), any());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private static class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestApiKeyAuthenticationToken() {
            super(-1, "public");
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.PLATFORM_API_KEY;
        }

        @Override
        public @Nullable PlatformType getPlatformType() {
            return null;
        }
    }
}
