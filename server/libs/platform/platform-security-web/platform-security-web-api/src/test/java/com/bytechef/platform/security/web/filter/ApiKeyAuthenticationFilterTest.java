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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

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
    void testDoFilterContinuesChainWhenAuthenticationSucceeds() throws Exception {
        TestApiKeyAuthenticationToken authenticatedToken = new TestApiKeyAuthenticationToken();

        authenticatedToken.setAuthenticated(true);

        when(authenticationManager.authenticate(any())).thenReturn(authenticatedToken);

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void testDoFilterPropagatesNonAuthenticationFailures() {
        when(authenticationManager.authenticate(any())).thenThrow(new IllegalStateException("Database is down"));

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(filterChain.getRequest()).isNull();
    }

    private static class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestApiKeyAuthenticationToken() {
            super(-1, "public");
        }
    }
}
