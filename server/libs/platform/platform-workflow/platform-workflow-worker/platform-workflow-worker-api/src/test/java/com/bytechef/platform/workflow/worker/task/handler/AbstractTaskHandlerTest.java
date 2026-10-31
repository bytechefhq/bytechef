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

package com.bytechef.platform.workflow.worker.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AbstractTaskHandlerTest {

    private static final long PROJECT_DEPLOYMENT_ID = 7L;

    private final ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
    private final JobPrincipalAuthenticationResolver jobPrincipalAuthenticationResolver = mock(
        JobPrincipalAuthenticationResolver.class);

    private final AbstractTaskHandler taskHandler = new AbstractTaskHandler(
        "aiAgentUtils", 1, "createAiSkill", actionDefinitionFacade,
        new JobPrincipalAuthenticationRunner(List.of(jobPrincipalAuthenticationResolver))) {};

    @BeforeEach
    void beforeEach() {
        when(jobPrincipalAuthenticationResolver.getType()).thenReturn(PlatformType.AUTOMATION);
        when(jobPrincipalAuthenticationResolver.isApplicable(anyLong())).thenReturn(true);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHandleRunsAsTheDeploymentOwnerWhenNoUserIsAuthenticated() throws Exception {
        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(ownerAuthentication()));

        stubExecutePerformReturningCurrentAuthentication();

        Authentication authentication = (Authentication) taskHandler.handle(taskExecution());

        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(authorityNames(authentication)).containsExactly("ROLE_USER");
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    @Test
    void testHandleRunsAsSystemWithoutAdminAuthorityWhenNoOwnerResolves() throws Exception {
        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.empty());

        stubExecutePerformReturningCurrentAuthentication();

        Authentication authentication = (Authentication) taskHandler.handle(taskExecution());

        assertThat(authentication.getName()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(authorityNames(authentication)).doesNotContain(AuthorityConstants.ADMIN);
        assertThat(authorityNames(authentication)).isEmpty();
    }

    @Test
    void testHandleKeepsTheAuthenticatedUser() throws Exception {
        Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
            "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        setAuthentication(userAuthentication);

        stubExecutePerformReturningCurrentAuthentication();

        assertThat(taskHandler.handle(taskExecution())).isSameAs(userAuthentication);

        verify(jobPrincipalAuthenticationResolver, never()).fetchAuthentication(anyLong());
    }

    @Test
    void testHandleTreatsAnAnonymousAuthenticationAsUnattended() throws Exception {
        setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));

        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(ownerAuthentication()));

        stubExecutePerformReturningCurrentAuthentication();

        Authentication authentication = (Authentication) taskHandler.handle(taskExecution());

        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(authorityNames(authentication)).containsExactly("ROLE_USER");
    }

    @Test
    void testHandleTreatsTheAdminSystemPrincipalOfASchedulerJobAsUnattended() throws Exception {
        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(ownerAuthentication()));

        stubExecutePerformReturningCurrentAuthentication();

        Authentication authentication = SecurityUtils.runAsSystem(() -> {
            try {
                return (Authentication) taskHandler.handle(taskExecution());
            } catch (TaskExecutionException taskExecutionException) {
                throw new IllegalStateException(taskExecutionException);
            }
        });

        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(authorityNames(authentication)).doesNotContain(AuthorityConstants.ADMIN);
    }

    @Test
    void testHandleRestoresTheSecurityContextAfterTheHandlerThrows() {
        Authentication anonymousAuthentication = anonymousAuthentication();

        setAuthentication(anonymousAuthentication);

        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(ownerAuthentication()));
        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("perform failed"));

        assertThatThrownBy(() -> taskHandler.handle(taskExecution()))
            .isInstanceOf(TaskExecutionException.class)
            .hasMessage("perform failed");

        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isSameAs(anonymousAuthentication);
    }

    private void stubExecutePerformReturningCurrentAuthentication() {
        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        SecurityContext securityContext = SecurityContextHolder.getContext();

                        return securityContext.getAuthentication();
                    });
    }

    private static Authentication anonymousAuthentication() {
        return new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority(AuthorityConstants.ANONYMOUS)));
    }

    private static List<String> authorityNames(Authentication authentication) {
        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }

    private static Authentication ownerAuthentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
            "owner", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);
    }

    private static TaskExecution taskExecution() {
        return TaskExecution.builder()
            .id(2L)
            .jobId(1L)
            .metadata(
                Map.of(
                    MetadataConstants.JOB_PRINCIPAL_ID, PROJECT_DEPLOYMENT_ID,
                    MetadataConstants.TYPE, PlatformType.AUTOMATION))
            .workflowTask(new WorkflowTask(Map.of("name", "createAiSkill", "type", "aiAgentUtils/v1/createAiSkill")))
            .build();
    }
}
