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

package com.bytechef.platform.workflow.worker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class JobPrincipalAuthenticationRunnerTest {

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRunUsesTheFirstApplicableResolverInOrder() {
        JobPrincipalAuthenticationResolver fallbackResolver = mock(JobPrincipalAuthenticationResolver.class);

        when(fallbackResolver.getType()).thenReturn(PlatformType.AUTOMATION);
        when(fallbackResolver.isApplicable(3L)).thenReturn(true);
        when(fallbackResolver.fetchAuthentication(3L)).thenReturn(Optional.of(authentication("project-owner")));

        JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
            List.of(fallbackResolver, new ConnectedUserResolver(true)));

        Authentication authentication = jobPrincipalAuthenticationRunner.run(
            PlatformType.AUTOMATION, 3L, "job 1", JobPrincipalAuthenticationRunnerTest::currentAuthentication);

        assertThat(authentication.getName()).isEqualTo("connected-user");

        verify(fallbackResolver, never()).fetchAuthentication(3L);
    }

    @Test
    void testRunSkipsAResolverThatIsNotApplicable() {
        JobPrincipalAuthenticationResolver fallbackResolver = mock(JobPrincipalAuthenticationResolver.class);

        when(fallbackResolver.getType()).thenReturn(PlatformType.AUTOMATION);
        when(fallbackResolver.isApplicable(3L)).thenReturn(true);
        when(fallbackResolver.fetchAuthentication(3L)).thenReturn(Optional.of(authentication("project-owner")));

        JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
            List.of(fallbackResolver, new ConnectedUserResolver(false)));

        Authentication authentication = jobPrincipalAuthenticationRunner.run(
            PlatformType.AUTOMATION, 3L, "job 1", JobPrincipalAuthenticationRunnerTest::currentAuthentication);

        assertThat(authentication.getName()).isEqualTo("project-owner");
    }

    @Test
    void testRunFallsBackToSystemWithoutAuthoritiesForAnUnknownPrincipal() {
        JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
            List.of(new ConnectedUserResolver(true)));

        Authentication authentication = jobPrincipalAuthenticationRunner.run(
            PlatformType.EMBEDDED, 3L, "job 1", JobPrincipalAuthenticationRunnerTest::currentAuthentication);

        assertThat(authentication.getName()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(authentication.getAuthorities()).isEmpty();

        authentication = jobPrincipalAuthenticationRunner.run(
            null, null, "job 1", JobPrincipalAuthenticationRunnerTest::currentAuthentication);

        assertThat(authentication.getName()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    private static Authentication authentication(String login) {
        return UsernamePasswordAuthenticationToken.authenticated(login, null, List.of());
    }

    private static Authentication currentAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return securityContext.getAuthentication();
    }

    @Order(1)
    private record ConnectedUserResolver(boolean applicable) implements JobPrincipalAuthenticationResolver {

        @Override
        public Optional<Authentication> fetchAuthentication(long jobPrincipalId) {
            return Optional.of(authentication("connected-user"));
        }

        @Override
        public PlatformType getType() {
            return PlatformType.AUTOMATION;
        }

        @Override
        public boolean isApplicable(long jobPrincipalId) {
            return applicable;
        }
    }
}
