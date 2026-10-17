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

package com.bytechef.automation.configuration.subflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ChildJobPrincipalFactoryIntTest {

    private static final long PARENT_JOB_ID = 100L;

    @Autowired
    private ChildJobPrincipalFactory childJobPrincipalFactory;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        reset(principalJobFacade, principalJobService);

        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testChildOfTheSameProjectIsLinkedToTheCallerDeployment() {
        Project project = saveProject("parent", workspace);

        long projectDeploymentId = saveDeployment(project, Environment.PRODUCTION, true, "parentWorkflow", "child");

        givenParentPrincipal(projectDeploymentId);

        childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, childJobParameters("child"));

        verify(principalJobFacade).createJob(
            any(JobParametersDTO.class), eq(projectDeploymentId), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testChildOfAnotherProjectIsLinkedToItsOwnDeploymentInTheSameEnvironment() {
        Project parentProject = saveProject("parent", workspace);
        Project childProject = saveProject("child", workspace);

        long parentProjectDeploymentId = saveDeployment(
            parentProject, Environment.PRODUCTION, true, "parentWorkflow");

        saveDeployment(childProject, Environment.DEVELOPMENT, true, "childWorkflow");

        long childProjectDeploymentId = saveDeployment(
            childProject, Environment.PRODUCTION, true, "childWorkflow");

        givenParentPrincipal(parentProjectDeploymentId);

        childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, childJobParameters("childWorkflow"));

        verify(principalJobFacade).createJob(
            any(JobParametersDTO.class), eq(childProjectDeploymentId), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testChildOfAProjectWithoutADeploymentInTheCallerEnvironmentFailsBeforeAnyJobIsCreated() {
        Project parentProject = saveProject("parent", workspace);
        Project childProject = saveProject("child", workspace);

        long parentProjectDeploymentId = saveDeployment(
            parentProject, Environment.PRODUCTION, true, "parentWorkflow");

        saveProjectWorkflow(childProject, "childWorkflow");
        saveDeployment(childProject, Environment.DEVELOPMENT, true, "childWorkflow");

        givenParentPrincipal(parentProjectDeploymentId);

        assertThatThrownBy(
            () -> childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, childJobParameters("childWorkflow")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'child'")
                .hasMessageContaining("PRODUCTION");

        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
        verify(principalJobFacade, never()).createChildJob(anyLong(), any(), any());
    }

    @Test
    void testChildOfADisabledDeploymentFailsBeforeAnyJobIsCreated() {
        Project parentProject = saveProject("parent", workspace);
        Project childProject = saveProject("child", workspace);

        long parentProjectDeploymentId = saveDeployment(
            parentProject, Environment.PRODUCTION, true, "parentWorkflow");

        saveDeployment(childProject, Environment.PRODUCTION, false, "childWorkflow");

        givenParentPrincipal(parentProjectDeploymentId);

        assertThatThrownBy(
            () -> childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, childJobParameters("childWorkflow")))
                .isInstanceOf(IllegalStateException.class);

        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testChildOfAProjectInAnotherWorkspaceIsRejected() {
        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other"));

        Project parentProject = saveProject("parent", workspace);
        Project childProject = saveProject("child", otherWorkspace);

        long parentProjectDeploymentId = saveDeployment(
            parentProject, Environment.PRODUCTION, true, "parentWorkflow");

        saveDeployment(childProject, Environment.PRODUCTION, true, "childWorkflow");

        givenParentPrincipal(parentProjectDeploymentId);

        assertThatThrownBy(
            () -> childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, childJobParameters("childWorkflow")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another workspace");

        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testChildOfAParentWithoutAPrincipalKeepsTheParentLinking() {
        JobParametersDTO jobParametersDTO = childJobParameters("childWorkflow");

        when(principalJobService.fetchJobPrincipalId(PARENT_JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        childJobPrincipalFactory.createChildJob(PARENT_JOB_ID, jobParametersDTO);

        verify(principalJobFacade).createChildJob(PARENT_JOB_ID, jobParametersDTO, PlatformType.AUTOMATION);
    }

    private void givenParentPrincipal(long projectDeploymentId) {
        when(principalJobService.fetchJobPrincipalId(PARENT_JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(projectDeploymentId));
    }

    private static JobParametersDTO childJobParameters(String workflowId) {
        return new JobParametersDTO(workflowId, 5L, Map.of());
    }

    private long saveDeployment(Project project, Environment environment, boolean enabled, String... workflowIds) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(enabled);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(project.getName() + "-" + environment);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());

        for (String workflowId : workflowIds) {
            if (projectWorkflowRepository.findByWorkflowId(workflowId)
                .isEmpty()) {

                saveProjectWorkflow(project, workflowId);
            }

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setEnabled(true);
            projectDeploymentWorkflow.setInputs(Map.of());
            projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
            projectDeploymentWorkflow.setWorkflowId(workflowId);

            projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
        }

        return projectDeploymentId;
    }

    private Project saveProject(String name, Workspace projectWorkspace) {
        return projectRepository.save(
            Project.builder()
                .name(name)
                .workspaceId(projectWorkspace.getId())
                .build());
    }

    private void saveProjectWorkflow(Project project, String workflowId) {
        projectWorkflowRepository.save(new ProjectWorkflow(Objects.requireNonNull(project.getId()), 1, workflowId));
    }
}
