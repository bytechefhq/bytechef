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

package com.bytechef.automation.ai.a2a.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aMethodSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class A2aProjectWorkflowServiceIntTest {

    @Nested
    @SpringBootTest(classes = A2aMethodSecurityIntTestConfiguration.class)
    class MethodSecurity {

        @Autowired
        private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

        @Autowired
        private A2aProjectWorkflowService a2aProjectWorkflowService;

        @BeforeEach
        void beforeEach() {
            when(a2aProjectWorkflowRepository.findById(5L)).thenReturn(Optional.of(new A2aProjectWorkflow(2L, 3L)));
            when(a2aProjectWorkflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            reset(a2aProjectWorkflowRepository);
        }

        @Test
        void testNonAdminCannotUpdateASkill() {
            authenticate("ROLE_USER");

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aProjectWorkflowService.updateSkill(5L, "Summarize", null));

            verify(a2aProjectWorkflowRepository, never()).findById(anyLong());
            verify(a2aProjectWorkflowRepository, never()).save(any());
        }

        @Test
        void testAdminCanUpdateASkill() {
            authenticate("ROLE_ADMIN");

            A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflowService.updateSkill(5L, "Summarize", null);

            assertThat(a2aProjectWorkflow.getSkillName()).isEqualTo("Summarize");

            verify(a2aProjectWorkflowRepository).save(a2aProjectWorkflow);
        }

        @Test
        void testNonAdminCannotUpdateEnabled() {
            authenticate("ROLE_USER");

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aProjectWorkflowService.updateEnabled(5L, false));

            verify(a2aProjectWorkflowRepository, never()).findById(anyLong());
            verify(a2aProjectWorkflowRepository, never()).save(any());
        }

        @Test
        void testAdminCanDisableAndReEnableAWorkflow() {
            authenticate("ROLE_ADMIN");

            A2aProjectWorkflow disabledA2aProjectWorkflow = a2aProjectWorkflowService.updateEnabled(5L, false);

            assertThat(disabledA2aProjectWorkflow.isEnabled()).isFalse();

            verify(a2aProjectWorkflowRepository).save(disabledA2aProjectWorkflow);

            A2aProjectWorkflow enabledA2aProjectWorkflow = a2aProjectWorkflowService.updateEnabled(5L, true);

            assertThat(enabledA2aProjectWorkflow.isEnabled()).isTrue();

            verify(a2aProjectWorkflowRepository, times(2)).save(enabledA2aProjectWorkflow);
        }
    }

    @Nested
    @SpringBootTest(classes = A2aIntTestConfiguration.class)
    @Import(PostgreSQLContainerConfiguration.class)
    @A2aIntTestConfigurationSharedMocks
    class Persistence {

        @Autowired
        private A2aProjectRepository a2aProjectRepository;

        @Autowired
        private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

        @Autowired
        private A2aProjectWorkflowService a2aProjectWorkflowService;

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private ProjectDeploymentRepository projectDeploymentRepository;

        @Autowired
        private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private WorkspaceRepository workspaceRepository;

        private A2aProject a2aProject;
        private ProjectDeploymentWorkflow projectDeploymentWorkflow;

        @BeforeEach
        void beforeEach() {
            A2aServer a2aServer = a2aServerRepository.save(
                new A2aServer("test-agent", null, Environment.DEVELOPMENT));

            Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

            Project project = Project.builder()
                .description("test-project")
                .name("test-project")
                .workspaceId(workspace.getId())
                .build();

            project = projectRepository.save(project);

            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setName("test-deployment");
            projectDeployment.setDescription("test deployment");
            projectDeployment.setEnabled(true);
            projectDeployment.setEnvironment(Environment.DEVELOPMENT);
            projectDeployment.setProjectId(project.getId());
            projectDeployment.setProjectVersion(1);

            projectDeployment = projectDeploymentRepository.save(projectDeployment);

            ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            newProjectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
            newProjectDeploymentWorkflow.setWorkflowId("test-workflow");

            projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(newProjectDeploymentWorkflow);

            a2aProject = a2aProjectRepository.save(
                new A2aProject(projectDeployment.getId(), a2aServer.getId(), project.getId()));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            a2aProjectWorkflowRepository.deleteAll();
            a2aProjectRepository.deleteAll();
            projectDeploymentWorkflowRepository.deleteAll();
            projectDeploymentRepository.deleteAll();
            projectRepository.deleteAll();
            workspaceRepository.deleteAll();
            a2aServerRepository.deleteAll();
        }

        @Test
        void testNewA2aProjectWorkflowIsStoredEnabled() {
            A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflowRepository.save(
                new A2aProjectWorkflow(a2aProject.getId(), projectDeploymentWorkflow.getId()));

            assertThat(a2aProjectWorkflowRepository.findById(Validate.notNull(a2aProjectWorkflow.getId(), "id")))
                .get()
                .extracting(A2aProjectWorkflow::isEnabled)
                .isEqualTo(true);
        }

        @Test
        void testUpdateEnabledPersistsTheFlag() {
            A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflowRepository.save(
                new A2aProjectWorkflow(a2aProject.getId(), projectDeploymentWorkflow.getId()));

            long a2aProjectWorkflowId = Validate.notNull(a2aProjectWorkflow.getId(), "id");

            authenticate("ROLE_ADMIN");

            a2aProjectWorkflowService.updateEnabled(a2aProjectWorkflowId, false);

            assertThat(a2aProjectWorkflowRepository.findById(a2aProjectWorkflowId))
                .get()
                .extracting(A2aProjectWorkflow::isEnabled)
                .isEqualTo(false);

            a2aProjectWorkflowService.updateEnabled(a2aProjectWorkflowId, true);

            assertThat(a2aProjectWorkflowRepository.findById(a2aProjectWorkflowId))
                .get()
                .extracting(A2aProjectWorkflow::isEnabled)
                .isEqualTo(true);
        }
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }
}
