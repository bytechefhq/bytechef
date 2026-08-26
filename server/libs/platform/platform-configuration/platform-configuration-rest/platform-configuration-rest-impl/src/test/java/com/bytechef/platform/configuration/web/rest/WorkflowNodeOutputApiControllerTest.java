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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * getPreviousWorkflowNodeOutputs is the interesting case: the facade method it reads through is {@code @Cacheable},
 * keyed from the raw method arguments, so environmentId is deliberately NOT resolved inside the facade (see
 * WorkflowNodeOutputFacadeImpl) -- it must be resolved here, once, before both the cache-eviction call
 * (checkWorkflowCache) and the cached read, so the two can never disagree about which environment's cache entry they
 * touch. This test asserts BOTH calls receive the identical effective value.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeOutputApiControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private WorkflowNodeOutputApiController controller;
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @BeforeEach
    void setUp() {
        ConversionService conversionService = mock(ConversionService.class);

        workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);
        controller = new WorkflowNodeOutputApiController(conversionService, workflowNodeOutputFacade);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsUsesConfinedPrincipalEnvironmentForBothEvictionAndRead() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(List.of());

        controller.getPreviousWorkflowNodeOutputs("workflow-1", DEVELOPMENT_ORDINAL, "node-1");

        ArgumentCaptor<Long> evictionEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> readEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeOutputFacade).checkWorkflowCache(
            eq("workflow-1"), eq("node-1"), evictionEnvironmentIdCaptor.capture());
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            eq("workflow-1"), eq("node-1"), readEnvironmentIdCaptor.capture());

        assertThat(evictionEnvironmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
        assertThat(readEnvironmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsHonoursSessionPrincipalRequestedEnvironmentForBoth() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(List.of());

        controller.getPreviousWorkflowNodeOutputs("workflow-1", DEVELOPMENT_ORDINAL, "node-1");

        ArgumentCaptor<Long> evictionEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> readEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeOutputFacade).checkWorkflowCache(
            eq("workflow-1"), eq("node-1"), evictionEnvironmentIdCaptor.capture());
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            eq("workflow-1"), eq("node-1"), readEnvironmentIdCaptor.capture());

        assertThat(evictionEnvironmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
        assertThat(readEnvironmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
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
