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

package com.bytechef.automation.ai.auto.memory.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@SpringJUnitConfig(AiAutoMemoryFacadeMethodSecurityTest.MethodSecurityConfiguration.class)
class AiAutoMemoryFacadeMethodSecurityTest {

    private static final long CURRENT_USER_ID = 7L;

    @Autowired
    private AiAutoMemoryFacade aiAutoMemoryFacade;

    @Autowired
    private AiAutoMemoryService aiAutoMemoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        reset(aiAutoMemoryService, userService, workspaceFacade);
    }

    @Test
    void testUnauthenticatedCallerIsRejected() {
        AiAutoMemoryPatch patch = new AiAutoMemoryPatch("Title", null, null, null);

        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> aiAutoMemoryFacade.getAiAutoMemories(1L, Environment.DEVELOPMENT, null, null));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> aiAutoMemoryFacade.fetchAiAutoMemory(1L, Environment.DEVELOPMENT, 3L, null));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> aiAutoMemoryFacade.getAiAutoMemoryPrincipals(1L, Environment.DEVELOPMENT));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(
                () -> aiAutoMemoryFacade.updateAiAutoMemory(1L, Environment.DEVELOPMENT, 3L, 4L, patch, null));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> aiAutoMemoryFacade.deleteAiAutoMemory(1L, Environment.DEVELOPMENT, 3L, null));

        verifyNoInteractions(aiAutoMemoryService, userService, workspaceFacade);
    }

    @Test
    void testAuthenticatedNonMemberIsRejected() {
        authenticate();

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> aiAutoMemoryFacade.getAiAutoMemories(2L, Environment.DEVELOPMENT, null, null));

        verifyNoInteractions(aiAutoMemoryService);
    }

    @Test
    void testAuthenticatedMemberReadsTheirOwnMemories() {
        authenticate();

        assertThat(aiAutoMemoryFacade.getAiAutoMemoryPrincipals(1L, Environment.DEVELOPMENT)).isEmpty();
    }

    private void authenticate() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        User user = new User();

        user.setId(CURRENT_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);

        when(workspaceFacade.getUserWorkspaces(CURRENT_USER_ID)).thenReturn(List.of(workspace));
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        AiAutoMemoryService aiAutoMemoryService() {
            return mock(AiAutoMemoryService.class);
        }

        @Bean
        AiAutoMemoryFacade aiAutoMemoryFacade(
            AiAutoMemoryService aiAutoMemoryService, UserService userService, WorkspaceFacade workspaceFacade) {

            return new AiAutoMemoryFacadeImpl(
                aiAutoMemoryService, mock(ProjectDeploymentFacade.class), mock(ProjectDeploymentService.class),
                userService, workspaceFacade);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        WorkspaceFacade workspaceFacade() {
            return mock(WorkspaceFacade.class);
        }
    }
}
