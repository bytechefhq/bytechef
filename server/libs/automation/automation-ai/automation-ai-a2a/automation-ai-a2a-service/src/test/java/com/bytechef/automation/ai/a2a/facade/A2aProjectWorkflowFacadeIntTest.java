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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.config.A2aMethodSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = A2aMethodSecurityIntTestConfiguration.class)
class A2aProjectWorkflowFacadeIntTest {

    @Autowired
    private A2aProjectWorkflowFacade a2aProjectWorkflowFacade;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        when(a2aProjectWorkflowRepository.findById(5L)).thenReturn(Optional.of(new A2aProjectWorkflow(2L, 3L)));
        when(a2aProjectWorkflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        reset(a2aProjectWorkflowRepository, permissionService);
    }

    @Test
    void testCallerWithoutTheA2aEditScopeCannotUpdateASkillOrEnabled() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectWorkflowFacade.updateA2aProjectWorkflowSkill(5L, "Summarize", null));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectWorkflowFacade.updateA2aProjectWorkflowEnabled(5L, false));

        verify(permissionService, times(2)).hasResourceScope(5L, "A2aProjectWorkflow", "A2A_EDIT");
        verify(a2aProjectWorkflowRepository, never()).findById(anyLong());
        verify(a2aProjectWorkflowRepository, never()).save(any());
    }

    @Test
    void testCallerWithoutTheA2aViewScopeCannotListTheWorkflowsOfAProject() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectWorkflowFacade.getA2aProjectA2aProjectWorkflows(2L));

        verify(permissionService).hasResourceScope(2L, "A2aProject", "A2A_VIEW");
    }

    @Test
    void testWorkspaceEditorUpdatesASkill() {
        when(permissionService.hasResourceScope(5L, "A2aProjectWorkflow", "A2A_EDIT")).thenReturn(true);

        A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflowFacade.updateA2aProjectWorkflowSkill(
            5L, "Summarize", null);

        assertThat(a2aProjectWorkflow.getSkillName()).isEqualTo("Summarize");

        verify(a2aProjectWorkflowRepository).save(a2aProjectWorkflow);
    }

    @Test
    void testWorkspaceEditorDisablesAndReEnablesAWorkflow() {
        when(permissionService.hasResourceScope(5L, "A2aProjectWorkflow", "A2A_EDIT")).thenReturn(true);

        A2aProjectWorkflow disabledA2aProjectWorkflow = a2aProjectWorkflowFacade.updateA2aProjectWorkflowEnabled(
            5L, false);

        assertThat(disabledA2aProjectWorkflow.isEnabled()).isFalse();

        A2aProjectWorkflow enabledA2aProjectWorkflow = a2aProjectWorkflowFacade.updateA2aProjectWorkflowEnabled(
            5L, true);

        assertThat(enabledA2aProjectWorkflow.isEnabled()).isTrue();

        verify(a2aProjectWorkflowRepository, times(2)).save(enabledA2aProjectWorkflow);
    }

    private static void authenticate() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
