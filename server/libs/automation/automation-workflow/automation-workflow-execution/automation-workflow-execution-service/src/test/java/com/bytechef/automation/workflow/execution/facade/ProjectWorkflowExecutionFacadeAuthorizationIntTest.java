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

package com.bytechef.automation.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectWorkflowExecutionFacadeAuthorizationIntTest.ProjectWorkflowExecutionFacadeAuthorizationIntTestConfiguration.class)
@MockitoBean(types = {
    ComponentDefinitionService.class, ContextService.class, EnvironmentService.class, Evaluator.class,
    PrincipalJobService.class, ProjectDeploymentService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowService.class, TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TaskFileStorage.class, TriggerExecutionService.class, TriggerFileStorage.class,
    WorkflowExecutionRowService.class, WorkflowService.class
})
class ProjectWorkflowExecutionFacadeAuthorizationIntTest {

    private static final long WORKSPACE_ID = 7L;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testNonMemberIsDeniedEveryWorkspaceScopedReadBeforeAnyLookup() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkspaceWorkflowExecution(
                WORKSPACE_ID, Environment.DEVELOPMENT.ordinal(), 1L))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkspaceWorkflowExecutions(
                WORKSPACE_ID, Environment.DEVELOPMENT.ordinal(), null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(jobService);
    }

    @Test
    void testMemberPassesTheGate() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkspaceWorkflowExecution(
                WORKSPACE_ID, Environment.DEVELOPMENT.ordinal(), 1L))
                    .isInstanceOf(AccessDeniedException.class);

        verify(jobService).fetchJob(1L);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(ProjectWorkflowExecutionFacadeImpl.class)
    static class ProjectWorkflowExecutionFacadeAuthorizationIntTestConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
