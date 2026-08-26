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

package com.bytechef.platform.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * checkWorkflowNodeTestOutputExists' hasPermission(#workflowId, 'Workflow', ...) gate is environment-agnostic, so the
 * caller-supplied environmentId is never checked -- and until this fix, the repository queries did not even use it,
 * making it a cross-environment existence oracle regardless of what the gate checked. This test pins the fixed
 * behavior: the environment reaching the repository call is the principal's own for a confined caller, and the
 * requested one for a session caller.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowNodeTestOutputServiceEnvironmentTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private WorkflowCacheManager workflowCacheManager;

    @Mock
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    private WorkflowNodeTestOutputServiceImpl workflowNodeTestOutputService;

    @BeforeEach
    void setUp() {
        workflowNodeTestOutputService = new WorkflowNodeTestOutputServiceImpl(
            cacheManager, workflowCacheManager, workflowNodeTestOutputRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCheckWorkflowNodeTestOutputExistsUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputRepository.existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            anyString(), anyString(), anyLong())).thenReturn(true);

        workflowNodeTestOutputService.checkWorkflowNodeTestOutputExists(
            "workflow-1", "node-1", null, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputRepository).existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testCheckWorkflowNodeTestOutputExistsHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputRepository.existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            anyString(), anyString(), anyLong())).thenReturn(true);

        workflowNodeTestOutputService.checkWorkflowNodeTestOutputExists(
            "workflow-1", "node-1", null, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputRepository).existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    /**
     * The createdDate != null branch -- the ...AndLastModifiedDateAfter derived query.
     * checkWorkflowNodeTestOutputExists has exactly one caller, WorkflowNodeTestOutputApiController (the editor's poll
     * for a webhook test result), which passes createdDate whenever the client supplies one.
     */
    @Test
    void testCheckWorkflowNodeTestOutputExistsWithCreatedDateUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        Instant createdDate = Instant.now();

        when(workflowNodeTestOutputRepository
            .existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentIdAndLastModifiedDateAfter(
                anyString(), anyString(), anyLong(), any(Instant.class)))
                    .thenReturn(true);

        workflowNodeTestOutputService.checkWorkflowNodeTestOutputExists(
            "workflow-1", "node-1", createdDate, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputRepository)
            .existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentIdAndLastModifiedDateAfter(
                eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture(), eq(createdDate));

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testCheckWorkflowNodeTestOutputExistsWithCreatedDateHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        Instant createdDate = Instant.now();

        when(workflowNodeTestOutputRepository
            .existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentIdAndLastModifiedDateAfter(
                anyString(), anyString(), anyLong(), any(Instant.class)))
                    .thenReturn(true);

        workflowNodeTestOutputService.checkWorkflowNodeTestOutputExists(
            "workflow-1", "node-1", createdDate, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputRepository)
            .existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentIdAndLastModifiedDateAfter(
                eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture(), eq(createdDate));

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
