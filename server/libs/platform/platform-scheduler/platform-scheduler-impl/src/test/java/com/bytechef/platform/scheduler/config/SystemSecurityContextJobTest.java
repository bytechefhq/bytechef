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

package com.bytechef.platform.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobExecutionException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class SystemSecurityContextJobTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExecuteRunsDelegateAsAuthenticatedSystemAdmin() throws JobExecutionException {
        AtomicReference<String> observedLogin = new AtomicReference<>();
        AtomicBoolean observedAdmin = new AtomicBoolean();
        AtomicBoolean observedAuthenticated = new AtomicBoolean();

        Job delegate = context -> {
            observedLogin.set(SecurityUtils.getCurrentUserLogin());
            observedAdmin.set(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));
            observedAuthenticated.set(SecurityUtils.isAuthenticated());
        };

        // Mirrors a Quartz worker thread that starts with no HTTP-derived SecurityContext.
        new SystemSecurityContextJob(delegate).execute(null);

        assertThat(observedAuthenticated.get()).isTrue();
        assertThat(observedLogin.get()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(observedAdmin.get()).isTrue();
    }

    @Test
    void testExecuteRestoresContextAfterCompletion() throws JobExecutionException {
        new SystemSecurityContextJob(context -> {}).execute(null);

        // The override must not leak onto the (pooled) worker thread once the job finishes.
        assertThat(SecurityUtils.isAuthenticated()).isFalse();
    }

    @Test
    void testExecuteRestoresPreExistingContextAfterCompletion() throws JobExecutionException {
        SecurityContext originalContext = SecurityContextHolder.createEmptyContext();

        originalContext.setAuthentication(new UsernamePasswordAuthenticationToken("original-user", ""));

        SecurityContextHolder.setContext(originalContext);

        new SystemSecurityContextJob(context -> {}).execute(null);

        assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("original-user");
        assertThat(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)).isFalse();
    }

    @Test
    void testExecutePropagatesJobExecutionExceptionUnwrapped() {
        JobExecutionException thrown = new JobExecutionException("boom");

        Job delegate = context -> {
            throw thrown;
        };

        Throwable caught = catchThrowable(() -> new SystemSecurityContextJob(delegate).execute(null));

        // The checked exception must surface to Quartz as-is, not wrapped in the bridging RuntimeException.
        assertThat(caught).isSameAs(thrown);
    }

    @Test
    void testExecuteRestoresContextWhenDelegateThrows() {
        JobExecutionException thrown = new JobExecutionException("boom");

        Job delegate = context -> {
            throw thrown;
        };

        catchThrowable(() -> new SystemSecurityContextJob(delegate).execute(null));

        assertThat(SecurityUtils.isAuthenticated()).isFalse();
    }
}
