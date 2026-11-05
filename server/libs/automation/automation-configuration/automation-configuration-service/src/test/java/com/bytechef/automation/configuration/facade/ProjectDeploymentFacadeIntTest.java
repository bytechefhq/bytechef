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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectDeploymentFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    private void renameProjectDeployment(long projectDeploymentId, String name) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        projectDeployment.setName(name);

        projectDeploymentRepository.save(projectDeployment);
    }

    @AfterEach
    public void afterEach() {
        SecurityContextHolder.clearContext();

        workspaceConnectionRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();

    }

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));

        projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
            projectWorkflowRepository);

        // Default stub: no running jobs unless explicitly mocked by a test
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        // Mock trigger definition service to throw for unknown triggers (prevents NPE in getStaticWebhookUrl)
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenThrow(new IllegalArgumentException("Trigger definition not found"));
    }

    @Disabled
    @Test
    public void testCreateProjectDeployment() {
        // TODO
    }

    @Test
    public void testCreateProjectDeploymentWithRequiredIntegerInputs() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO,
            """
                {
                    "label": "BUG - RunDaily - Scheduler",
                    "inputs": [
                        {"name": "destinationFolderName", "label": "Destination folder name", "type": "string"},
                        {"name": "serviceProviderEmail", "label": "Email", "type": "string", "required": true},
                        {"name": "hourToRun", "label": "Hour", "type": "integer", "required": true},
                        {"name": "minutesToRun", "label": "Minute", "type": "integer", "required": true}
                    ],
                    "triggers": [
                        {
                            "label": "Every Work Day",
                            "name": "trigger_1",
                            "type": "schedule/v1/everyDay",
                            "parameters": {
                                "hour": "${hourToRun}",
                                "minute": "${minutesToRun}",
                                "timezone": "Europe/Zagreb",
                                "dayOfWeek": [2, 3, 4, 5, 6]
                            }
                        }
                    ],
                    "tasks": [
                        {
                            "label": "Logger",
                            "name": "logger_1",
                            "type": "logger/v1/info",
                            "parameters": {"text": "Run every day at ${hourToRun}:${minutesToRun}"}
                        }
                    ]
                }""");

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        ProjectWorkflow publishedProjectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 1)
            .getFirst();

        Map<String, Object> inputs = Map.of(
            "serviceProviderEmail", "racuni@example.com",
            "hourToRun", 11,
            "minutesToRun", 50);

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, inputs, true, null, null, null, null, null, null, null, 0,
            publishedProjectWorkflow.getWorkflowId(), publishedProjectWorkflow.getUuidAsString());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectDTO.id())
            .name("GH-5404")
            .environment(Environment.DEVELOPMENT)
            .projectVersion(1)
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, publishedProjectWorkflow.getWorkflowId(), true);

        ProjectDeploymentDTO createdProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        assertThat(createdProjectDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.enabled()).isTrue();

                Map<String, ?> savedInputs = projectDeploymentWorkflow.inputs();

                assertThat(savedInputs.get("hourToRun")).isEqualTo(11);
                assertThat(savedInputs.get("minutesToRun")).isEqualTo(50);
                assertThat(savedInputs.get("serviceProviderEmail")).isEqualTo("racuni@example.com");
            });
    }

    @Disabled
    @Test
    public void testCreateProjectDeploymentJob() {
        // TODO
    }

    @Test
    public void testDeleteProjectDeployment() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        ProjectDeploymentDTO projectDeploymentToDelete =
            projectDeploymentFacade.getProjectDeployment(projectDeploymentDTO.id());

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentToDelete.id());

        List<ProjectDeploymentDTO> workspaceProjectDeployments = projectDeploymentFacade.getWorkspaceProjectDeployments(
            workspace.getId(), (long) Environment.DEVELOPMENT.ordinal(), projectDeploymentToDelete.projectId(), null,
            true);

        assertThat(workspaceProjectDeployments).hasSize(0);
    }

    @Test
    public void testGetWorkspaceProjectDeploymentsHidesDeploymentsOfEmbeddedAutomationWorkflowProjects() {
        ProjectDTO automationWorkflowProjectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDTO plainProjectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO automationWorkflowProjectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), automationWorkflowProjectDTO);
        ProjectDeploymentDTO plainDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), plainProjectDTO);

        renameProjectDeployment(automationWorkflowProjectDeploymentDTO.id(), "admin");
        renameProjectDeployment(plainDeploymentDTO.id(), "plain");

        testOwnerContributors.addAutomationWorkflowProjectId(automationWorkflowProjectDTO.id());

        List<ProjectDeploymentDTO> workspaceProjectDeployments = projectDeploymentFacade.getWorkspaceProjectDeployments(
            workspace.getId(), null, null, null, false);

        assertThat(workspaceProjectDeployments).extracting(ProjectDeploymentDTO::name)
            .contains("plain")
            .doesNotContain("admin");
    }

    @Test
    public void testDeleteProjectDeploymentRemovesAllPrincipalJobsBeforeAnyJob() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();
        long parentJobId = 501L;
        long childJobId = 502L;

        when(principalJobService.getJobIds(deploymentId, PlatformType.AUTOMATION))
            .thenReturn(List.of(parentJobId, childJobId));

        projectDeploymentFacade.deleteProjectDeployment(deploymentId);

        InOrder inOrder = inOrder(principalJobService, jobFacade);

        inOrder.verify(principalJobService)
            .deletePrincipalJobs(parentJobId, PlatformType.AUTOMATION);
        inOrder.verify(principalJobService)
            .deletePrincipalJobs(childJobId, PlatformType.AUTOMATION);
        inOrder.verify(jobFacade, calls(1))
            .deleteJob(anyLong());
        inOrder.verify(jobFacade, calls(1))
            .deleteJob(anyLong());

        verify(jobFacade).deleteJob(parentJobId);
        verify(jobFacade).deleteJob(childJobId);
    }

    @Disabled
    @Test
    public void testGetProjectDeployment() {
        // TODO
    }

    @Test
    public void testGetProjectDeploymentTags() {
        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDeploymentFacadeHelper.createProject(workspace.getId()));
        ProjectDeploymentDTO otherProjectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDeploymentFacadeHelper.createProject(workspace.getId()));

        projectDeploymentFacade.updateProjectDeploymentTags(
            projectDeploymentDTO.id(), List.of(new Tag("deployment-tag-x"), new Tag("deployment-tag-y")));
        projectDeploymentFacade.updateProjectDeploymentTags(
            otherProjectDeploymentDTO.id(), List.of(new Tag("deployment-tag-z")));

        List<Tag> tags = projectDeploymentFacade.getProjectDeploymentTags();

        assertThat(tags)
            .extracting(Tag::getName)
            .containsExactlyInAnyOrder("deployment-tag-x", "deployment-tag-y", "deployment-tag-z");
    }

    @Test
    public void testCreateProjectDeploymentRejectsMissingBlankAndNullRequiredInputs() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflow publishedProjectWorkflow = publishProjectWithInputsWorkflow(
            projectDTO, "{\"name\": \"name\", \"label\": \"Name\", \"type\": \"string\", \"required\": true}");

        Map<String, Object> nullValueInputs = new HashMap<>();

        nullValueInputs.put("name", null);

        List<Map<String, Object>> invalidInputs = List.of(Map.of(), Map.of("name", "   "), nullValueInputs);

        for (Map<String, Object> inputs : invalidInputs) {
            assertThatThrownBy(
                () -> createProjectDeploymentWithInputs(projectDTO, publishedProjectWorkflow, inputs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing required param: name");
        }

        assertThat(projectDeploymentRepository.findAll()).isEmpty();
    }

    @Test
    public void testCreateProjectDeploymentIgnoresAbsentOptionalInputs() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflow publishedProjectWorkflow = publishProjectWithInputsWorkflow(
            projectDTO, "{\"name\": \"destinationFolderName\", \"label\": \"Folder\", \"type\": \"string\"}");

        long projectDeploymentId = createProjectDeploymentWithInputs(projectDTO, publishedProjectWorkflow, Map.of());

        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .hasSize(1);
    }

    @Disabled
    @Test
    public void testSearchProjectDeployments() {
        // TODO
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToEnabledShouldDisableAndReEnable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // When - Update the same workflow (enabled to enabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is still enabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowDisabledToEnabledShouldOnlyEnable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

        // When - Enable the workflow (disabled to enabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is enabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testDisablingWorkflowStopsRunningJobs() {
        // Given - Create a project with a workflow and enable deployment + workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        projectDeploymentFacade.enableProjectDeployment(deploymentId, true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, true);

        // And mock PrincipalJobService to return STARTED job IDs for this deployment/workflow
        List<Long> runningJobIds = List.of(101L, 202L);

        when(
            principalJobService.getJobIds(
                eq(Job.Status.STARTED), eq(null), eq(null), eq(List.of(deploymentId)), eq(PlatformType.AUTOMATION),
                eq(List.of(workflowId)), eq(false), eq(0)))
                    .thenReturn(new PageImpl<>(runningJobIds, PageRequest.of(0, 20), runningJobIds.size()));

        // When - Disable the workflow
        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, false);

        // Then - verify each running job was stopped
        verify(jobFacade).stopJob(101L);
        verify(jobFacade).stopJob(202L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToDisabledShouldDisable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // When - Disable the workflow (enabled to disabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

        // Then - Verify the workflow is disabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isFalse();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowProjectDeploymentDisabledShouldNotAffectTriggers() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), false);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        // When - Enable the workflow while project deployment is disabled
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is enabled but triggers should not be affected
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.enabled()).isFalse();
        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowInputsReschedulesEnabledTriggers() {
        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?");

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, projectDeploymentWorkflow.getWorkflowId(), true);

        reset(triggerLifecycleFacade);

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
            projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setInputs(Map.of("cronExpression", "5/5 16 * * ?"));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "0/5 15 * * ?")), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "5/5 16 * * ?")), any(),
                any(), anyLong());

        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .first()
            .satisfies(
                workflow -> assertThat(workflow.getInputs()).isEqualTo(Map.of("cronExpression", "5/5 16 * * ?")));
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowInputsWithProjectDeploymentDisabledShouldNotAffectTriggers() {
        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?");

        reset(triggerLifecycleFacade);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setInputs(Map.of("cronExpression", "5/5 16 * * ?"));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowWithStaleVersionKeepsEnabledTriggers() {
        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?");

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, projectDeploymentWorkflow.getWorkflowId(), true);

        ProjectDeploymentWorkflow staleProjectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        ProjectDeploymentWorkflow currentProjectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        currentProjectDeploymentWorkflow.setInputs(Map.of("cronExpression", "10/5 15 * * ?"));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(currentProjectDeploymentWorkflow);

        reset(triggerLifecycleFacade);

        staleProjectDeploymentWorkflow.setInputs(Map.of("cronExpression", "5/5 16 * * ?"));

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeploymentWorkflow(staleProjectDeploymentWorkflow))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRestoresPreviousTriggersWhenEnableFails() {
        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?");

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, projectDeploymentWorkflow.getWorkflowId(), true);

        reset(triggerLifecycleFacade);

        doThrow(new IllegalStateException("Trigger registration failed"))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "5/5 16 * * ?")), any(),
                any(), anyLong());

        ProjectDeploymentWorkflow updatedProjectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        updatedProjectDeploymentWorkflow.setInputs(Map.of("cronExpression", "5/5 16 * * ?"));

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeploymentWorkflow(updatedProjectDeploymentWorkflow))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Trigger registration failed");

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "0/5 15 * * ?")), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "5/5 16 * * ?")), any(),
                any(), anyLong());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "5/5 16 * * ?")), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                any(), any(), any(),
                argThat(parameters -> Objects.equals(parameters.get("expression"), "0/5 15 * * ?")), any(),
                any(), anyLong());

        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .first()
            .satisfies(
                workflow -> assertThat(workflow.getInputs()).isEqualTo(Map.of("cronExpression", "0/5 15 * * ?")));
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowConnectionDisablesTriggersWithPreviousConnection() {
        long oldConnectionId = 1001L;
        long newConnectionId = 1002L;

        ComponentConnection componentConnection = new ComponentConnection(
            "schedule", 1, "trigger_1", "connection", false);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(List.of(componentConnection));
        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(componentConnection);

        Connection connection = new Connection();

        connection.setComponentName("schedule");

        when(connectionService.getConnection(anyLong())).thenReturn(connection);

        workspaceConnectionService.create(oldConnectionId, workspace.getId());
        workspaceConnectionService.create(newConnectionId, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment(
            "0/5 15 * * ?",
            List.of(new ProjectDeploymentWorkflowConnection(oldConnectionId, "connection", "trigger_1")));

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, projectDeploymentWorkflow.getWorkflowId(), true);

        reset(triggerLifecycleFacade);

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
            projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(newConnectionId, "connection", "trigger_1")));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), eq(oldConnectionId));
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), eq(newConnectionId), any(), anyLong());
    }

    @Disabled
    @Test
    public void testCreateProjectDeploymentRejectsAConnectionOfAnotherWorkspace() {
        stubScheduleConnection(1101L, Environment.DEVELOPMENT, foreignWorkspaceId());

        assertThatThrownBy(() -> createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1101L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testCreateProjectDeploymentRejectsAConnectionFromAnotherEnvironment() {
        stubScheduleConnection(1102L, Environment.PRODUCTION, workspace.getId());

        assertThatThrownBy(() -> createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1102L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testCreateProjectDeploymentLetsATenantAdminUseAConnectionOfAnotherWorkspace() {
        stubScheduleConnection(1103L, Environment.DEVELOPMENT, foreignWorkspaceId());

        authenticate(AuthorityConstants.ADMIN);

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1103L));

        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1103L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowAcceptsAConnectionOfTheProjectWorkspaceAndEnvironment() {
        stubScheduleConnection(1104L, Environment.DEVELOPMENT, workspace.getId());
        stubScheduleConnection(1105L, Environment.DEVELOPMENT, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1104L));

        updateProjectDeploymentWorkflowConnection(projectDeploymentId, 1105L);

        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1105L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRejectsAConnectionOfAnotherWorkspace() {
        stubScheduleConnection(1106L, Environment.DEVELOPMENT, workspace.getId());
        stubScheduleConnection(1107L, Environment.DEVELOPMENT, foreignWorkspaceId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1106L));

        assertThatThrownBy(() -> updateProjectDeploymentWorkflowConnection(projectDeploymentId, 1107L))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1106L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRejectsAConnectionFromAnotherEnvironment() {
        stubScheduleConnection(1108L, Environment.DEVELOPMENT, workspace.getId());
        stubScheduleConnection(1109L, Environment.PRODUCTION, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1108L));

        assertThatThrownBy(() -> updateProjectDeploymentWorkflowConnection(projectDeploymentId, 1109L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowKeepsAnUnchangedConnectionATenantAdminBound() {
        stubScheduleConnection(1110L, Environment.DEVELOPMENT, foreignWorkspaceId());

        authenticate(AuthorityConstants.ADMIN);

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1110L));

        SecurityContextHolder.clearContext();

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setInputs(Map.of("cronExpression", "0/10 15 * * ?"));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1110L);
    }

    @Test
    public void testCreateProjectDeploymentRejectsAWorkflowOfAnotherProject() {
        long foreignWorkspaceId = foreignWorkspaceId();

        stubScheduleConnection(1111L, Environment.DEVELOPMENT, foreignWorkspaceId);

        ProjectWorkflow projectWorkflow = publishCronInputProjectWorkflow(workspace.getId());
        ProjectWorkflow foreignProjectWorkflow = publishCronInputProjectWorkflow(foreignWorkspaceId);

        assertThatThrownBy(() -> createCronInputProjectDeployment(
            projectWorkflow.getProjectId(), foreignProjectWorkflow, "0/5 15 * * ?", scheduleConnections(1111L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to project");
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRejectsAWorkflowOfAnotherProject() {
        long foreignWorkspaceId = foreignWorkspaceId();

        stubScheduleConnection(1112L, Environment.DEVELOPMENT, workspace.getId());
        stubScheduleConnection(1113L, Environment.DEVELOPMENT, foreignWorkspaceId);

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1112L));

        ProjectWorkflow foreignProjectWorkflow = publishCronInputProjectWorkflow(foreignWorkspaceId);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        String workflowId = projectDeploymentWorkflow.getWorkflowId();

        projectDeploymentWorkflow.setConnections(scheduleConnections(1113L));
        projectDeploymentWorkflow.setWorkflowId(foreignProjectWorkflow.getWorkflowId());

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
            .isInstanceOf(IllegalArgumentException.class);

        ProjectDeploymentWorkflow storedProjectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        assertThat(storedProjectDeploymentWorkflow.getWorkflowId()).isEqualTo(workflowId);
        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1112L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRejectsARowOfAnotherProjectDeployment() {
        stubScheduleConnection(1114L, Environment.DEVELOPMENT, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1114L));
        long otherProjectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", List.of());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setProjectDeploymentId(otherProjectDeploymentId);

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUpdateProjectDeploymentRejectsADeploymentOfAnotherProject() {
        stubScheduleConnection(1115L, Environment.DEVELOPMENT, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1115L));

        ProjectWorkflow otherProjectWorkflow = publishCronInputProjectWorkflow(workspace.getId());

        ProjectDeploymentDTO currentProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        ProjectDeploymentDTO updateDTO = ProjectDeploymentDTO.builder()
            .id(projectDeploymentId)
            .projectId(otherProjectWorkflow.getProjectId())
            .name(currentProjectDeployment.name())
            .environment(Environment.DEVELOPMENT)
            .projectVersion(1)
            .projectDeploymentWorkflows(currentProjectDeployment.projectDeploymentWorkflows())
            .version(currentProjectDeployment.version())
            .build();

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeployment(updateDTO))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowRejectsMovingAnAdminBoundConnectionToAnotherSlot() {
        stubScheduleConnection(1116L, Environment.DEVELOPMENT, foreignWorkspaceId());

        authenticate(AuthorityConstants.ADMIN);

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1116L));

        SecurityContextHolder.clearContext();

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setConnections(
            List.of(
                new ProjectDeploymentWorkflowConnection(1116L, "connection", "trigger_1"),
                new ProjectDeploymentWorkflowConnection(1116L, "otherConnection", "trigger_1")));

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(getStoredConnectionIds(projectDeploymentId)).containsExactly(1116L);
    }

    @Test
    public void testUpdateProjectDeploymentRejectsTheDraftProjectVersion() {
        stubScheduleConnection(1117L, Environment.DEVELOPMENT, workspace.getId());

        long projectDeploymentId = createCronInputProjectDeployment("0/5 15 * * ?", scheduleConnections(1117L));

        ProjectDeploymentDTO existingProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        ProjectDeploymentDTO updateDTO = toToolProjectDeploymentDTO(
            existingProjectDeployment, existingProjectDeployment.name(), 2,
            existingProjectDeployment.projectDeploymentWorkflows());

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeployment(updateDTO))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("cannot be in DRAFT");

        assertThat(projectDeploymentFacade.getProjectDeployment(projectDeploymentId)
            .projectVersion()).isEqualTo(1);
    }

    @Test
    public void testRollbackToolPayloadKeepsTheDeploymentWorkflowsOnTheTargetVersion() {
        stubScheduleConnection(1118L, Environment.DEVELOPMENT, workspace.getId());

        ProjectWorkflow v1ProjectWorkflow = publishCronInputProjectWorkflow(workspace.getId());
        long projectId = v1ProjectWorkflow.getProjectId();

        projectFacade.publishProject(projectId, "Published v2 for test", false);

        ProjectWorkflow v2ProjectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectId, 2)
            .getFirst();

        long projectDeploymentId = createCronInputProjectDeployment(
            projectId, 2, v2ProjectWorkflow, "0/5 15 * * ?", scheduleConnections(1118L));

        ProjectDeploymentDTO existingProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        projectDeploymentFacade.updateProjectDeployment(
            toToolProjectDeploymentDTO(
                existingProjectDeployment, existingProjectDeployment.name(), 1,
                toTargetVersionWorkflows(projectId, 1, existingProjectDeployment.projectDeploymentWorkflows())));

        assertThat(projectDeploymentFacade.getProjectDeployment(projectDeploymentId)
            .projectVersion()).isEqualTo(1);
        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.getWorkflowId()).isEqualTo(v1ProjectWorkflow.getWorkflowId());
                assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(projectDeploymentWorkflow.getConnections())
                    .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .containsExactly(1118L);
                assertThat(projectDeploymentWorkflow.getInputs())
                    .isEqualTo(Map.of("cronExpression", "0/5 15 * * ?"));
            });
    }

    @Test
    public void testUpdateToolPayloadKeepsTheDeploymentWorkflows() {
        stubScheduleConnection(1119L, Environment.DEVELOPMENT, workspace.getId());

        ProjectWorkflow v1ProjectWorkflow = publishCronInputProjectWorkflow(workspace.getId());
        long projectId = v1ProjectWorkflow.getProjectId();

        projectFacade.publishProject(projectId, "Published v2 for test", false);

        ProjectWorkflow v2ProjectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectId, 2)
            .getFirst();

        long projectDeploymentId = createCronInputProjectDeployment(
            projectId, 2, v2ProjectWorkflow, "0/5 15 * * ?", scheduleConnections(1119L));

        ProjectDeploymentDTO existingProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        projectDeploymentFacade.updateProjectDeployment(
            toToolProjectDeploymentDTO(
                existingProjectDeployment, "Renamed Deployment", 2,
                existingProjectDeployment.projectDeploymentWorkflows()));

        ProjectDeploymentDTO updatedProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        assertThat(updatedProjectDeployment.name()).isEqualTo("Renamed Deployment");
        assertThat(updatedProjectDeployment.projectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.getWorkflowId()).isEqualTo(v2ProjectWorkflow.getWorkflowId());
                assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(projectDeploymentWorkflow.getConnections())
                    .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .containsExactly(1119L);
            });
    }

    private static ProjectDeploymentDTO toToolProjectDeploymentDTO(
        ProjectDeploymentDTO existing, String name, int projectVersion,
        List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows) {

        return ProjectDeploymentDTO.builder()
            .createdBy(existing.createdBy())
            .createdDate(existing.createdDate())
            .description(existing.description())
            .enabled(existing.enabled())
            .environment(existing.environment())
            .id(existing.id())
            .name(name)
            .lastModifiedBy(existing.lastModifiedBy())
            .lastModifiedDate(existing.lastModifiedDate())
            .project(existing.project())
            .projectId(existing.projectId())
            .projectVersion(projectVersion)
            .projectDeploymentWorkflows(projectDeploymentWorkflows)
            .tags(existing.tags())
            .version(existing.version())
            .build();
    }

    private List<ProjectDeploymentWorkflowDTO> toTargetVersionWorkflows(
        long projectId, int projectVersion, List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows) {

        List<ProjectWorkflow> targetProjectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectId, projectVersion);

        List<ProjectDeploymentWorkflowDTO> targetVersionWorkflows = new ArrayList<>();

        for (ProjectDeploymentWorkflowDTO projectDeploymentWorkflow : projectDeploymentWorkflows) {
            for (ProjectWorkflow projectWorkflow : targetProjectWorkflows) {
                if (Objects.equals(projectWorkflow.getUuidAsString(), projectDeploymentWorkflow.workflowUuid())) {
                    targetVersionWorkflows.add(
                        new ProjectDeploymentWorkflowDTO(
                            projectDeploymentWorkflow.connections(), null, null, projectDeploymentWorkflow.inputs(),
                            projectDeploymentWorkflow.enabled(), projectDeploymentWorkflow.id(), null, null, null,
                            null, projectDeploymentWorkflow.projectDeploymentId(), null,
                            projectDeploymentWorkflow.version(), projectWorkflow.getWorkflowId(),
                            projectDeploymentWorkflow.workflowUuid()));
                }
            }
        }

        return targetVersionWorkflows;
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private long foreignWorkspaceId() {
        Workspace foreignWorkspace = workspaceRepository.save(new Workspace("foreign"));

        return foreignWorkspace.getId();
    }

    private List<Long> getStoredConnectionIds(long projectDeploymentId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        return projectDeploymentWorkflow.getConnections()
            .stream()
            .map(ProjectDeploymentWorkflowConnection::getConnectionId)
            .toList();
    }

    private static List<ProjectDeploymentWorkflowConnection> scheduleConnections(long connectionId) {
        return List.of(new ProjectDeploymentWorkflowConnection(connectionId, "connection", "trigger_1"));
    }

    private void stubScheduleConnection(long connectionId, Environment environment, long workspaceId) {
        ComponentConnection componentConnection = new ComponentConnection(
            "schedule", 1, "trigger_1", "connection", false);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(List.of(componentConnection));
        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(componentConnection);

        Connection connection = new Connection();

        connection.setComponentName("schedule");
        connection.setEnvironmentId(environment.ordinal());
        connection.setId(connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);

        workspaceConnectionService.create(connectionId, workspaceId);
    }

    private void updateProjectDeploymentWorkflowConnection(long projectDeploymentId, long connectionId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentId)
            .getFirst();

        projectDeploymentWorkflow.setConnections(scheduleConnections(connectionId));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);
    }

    @Test
    public void testUpdateProjectDeploymentTags() {
        // TODO
    }

    @Test
    public void testWorkflowLastExecutionDateFiltersJobsByDeploymentId() {
        // Given - Create a project with a workflow and deploy it
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        // And mock job execution - simulate a job that ran in this deployment
        long jobIdForThisDeployment = 12345L;
        Instant executionTime = Instant.parse("2024-06-15T10:30:00Z");

        Job mockJob = new Job();

        mockJob.setEndDate(executionTime);

        when(principalJobService.fetchLastWorkflowJobId(eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForThisDeployment));
        when(jobService.getJob(jobIdForThisDeployment))
            .thenReturn(mockJob);

        // When - Get the project deployment (which fetches workflow execution dates)
        ProjectDeploymentDTO result = projectDeploymentFacade.getProjectDeployment(deploymentId);

        // Then - Verify that fetchLastWorkflowJobId was called with THIS deployment's ID
        // This ensures execution dates are filtered by deployment, not fetched globally
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION));

        // And the workflow should have the execution date from the mocked job
        assertThat(result.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime));
    }

    @Test
    public void testWorkflowLastExecutionDateIsolatedBetweenDeployments() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        // And create TWO deployments (simulating different environments)
        ProjectDeploymentDTO deployment1 = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        ProjectDeploymentDTO deployment2 = projectDeploymentFacadeHelper.createProjectDeploymentForEnvironment(
            workspace.getId(), projectDTO, Environment.STAGING);

        long deployment1Id = deployment1.id();
        long deployment2Id = deployment2.id();

        // And mock different job executions for each deployment
        long jobIdForDeployment1 = 1001L;
        long jobIdForDeployment2 = 2002L;
        Instant executionTime1 = Instant.parse("2024-06-01T10:00:00Z");
        Instant executionTime2 = Instant.parse("2024-06-15T15:00:00Z");

        Job mockJob1 = new Job();

        mockJob1.setEndDate(executionTime1);

        Job mockJob2 = new Job();

        mockJob2.setEndDate(executionTime2);

        // Reset mocks to clear default stub
        reset(principalJobService);

        when(principalJobService.fetchLastWorkflowJobId(eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment1));
        when(principalJobService.fetchLastWorkflowJobId(eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment2));

        when(jobService.getJob(jobIdForDeployment1))
            .thenReturn(mockJob1);
        when(jobService.getJob(jobIdForDeployment2))
            .thenReturn(mockJob2);

        // Re-stub getJobIds for other tests
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        // When - Get each deployment
        ProjectDeploymentDTO result1 = projectDeploymentFacade.getProjectDeployment(deployment1Id);
        ProjectDeploymentDTO result2 = projectDeploymentFacade.getProjectDeployment(deployment2Id);

        // Then - Each deployment should have its OWN execution date, not mixed up
        assertThat(result1.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime1));

        assertThat(result2.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime2));

        // Verify the correct deployment IDs were used in the queries
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION));
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION));
    }

    @Test
    public void testUpdateProjectVersionWithWorkflowDisabledShouldDisableTriggers() {
        // Given - Create a project with a trigger-enabled workflow and publish v1
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String v1WorkflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), v1WorkflowId, true);

        // And publish v2
        projectFacade.publishProject(projectDTO.id(), "Published v2 for test", false);

        // And get the v2 workflow ID and the workflow UUID
        List<ProjectWorkflow> v2ProjectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 2);

        ProjectWorkflow v2ProjectWorkflow = v2ProjectWorkflows.getFirst();

        String v2WorkflowId = v2ProjectWorkflow.getWorkflowId();
        String workflowUuid = v2ProjectWorkflow.getUuidAsString();

        // When - Reset mock to clear calls from the enable step, then update to v2 with workflow disabled
        reset(triggerLifecycleFacade);

        ProjectDeploymentWorkflowDTO disabledWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), false, null, null, null, null, null, null, null, 0, v2WorkflowId,
            workflowUuid);

        ProjectDeploymentDTO currentDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        ProjectDeploymentDTO updateDTO = ProjectDeploymentDTO.builder()
            .id(projectDeploymentDTO.id())
            .projectId(projectDTO.id())
            .name(projectDeploymentDTO.name())
            .enabled(true)
            .environment(projectDeploymentDTO.environment())
            .projectVersion(2)
            .projectDeploymentWorkflows(List.of(disabledWorkflowDTO))
            .version(currentDeployment.version())
            .build();

        projectDeploymentFacade.updateProjectDeployment(updateDTO);

        // Then - Verify the deployment workflow is now disabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> assertThat(workflow.enabled()).isFalse());

        // And verify that trigger disable was called for the previously-enabled workflow
        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerDisable(any(), any(), any(), any(), any());
    }

    @Test
    public void testUpdateProjectVersionWithWorkflowStillEnabledShouldReenableTriggers() {
        // Given - Create a project with a trigger-enabled workflow and publish v1
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String v1WorkflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), v1WorkflowId, true);

        // And publish v2
        projectFacade.publishProject(projectDTO.id(), "Published v2 for test", false);

        // And get the v2 workflow ID and the workflow UUID
        List<ProjectWorkflow> v2ProjectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 2);

        ProjectWorkflow v2ProjectWorkflow = v2ProjectWorkflows.getFirst();

        String v2WorkflowId = v2ProjectWorkflow.getWorkflowId();
        String workflowUuid = v2ProjectWorkflow.getUuidAsString();

        // When - Reset mock to clear calls from the enable step, then update to v2 with workflow still enabled
        reset(triggerLifecycleFacade);

        ProjectDeploymentWorkflowDTO enabledWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), true, null, null, null, null, null, null, null, 0, v2WorkflowId,
            workflowUuid);

        ProjectDeploymentDTO currentDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        ProjectDeploymentDTO updateDTO = ProjectDeploymentDTO.builder()
            .id(projectDeploymentDTO.id())
            .projectId(projectDTO.id())
            .name(projectDeploymentDTO.name())
            .enabled(true)
            .environment(projectDeploymentDTO.environment())
            .projectVersion(2)
            .projectDeploymentWorkflows(List.of(enabledWorkflowDTO))
            .version(currentDeployment.version())
            .build();

        projectDeploymentFacade.updateProjectDeployment(updateDTO);

        // Then - Verify the workflow is still enabled with the new v2 workflow ID
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(v2WorkflowId);
            });

        // And verify that old triggers were disabled and new triggers were enabled
        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerEnable(
            any(), any(), any(), any(), any(), any(), anyLong());
    }

    private long createProjectDeploymentWithInputs(
        ProjectDTO projectDTO, ProjectWorkflow publishedProjectWorkflow, Map<String, ?> inputs) {

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, inputs, true, null, null, null, null, null, null, null, 0,
            publishedProjectWorkflow.getWorkflowId(), publishedProjectWorkflow.getUuidAsString());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectDTO.id())
            .name("Inputs Deployment")
            .environment(Environment.DEVELOPMENT)
            .projectVersion(1)
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        return projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO);
    }

    private ProjectWorkflow publishProjectWithInputsWorkflow(ProjectDTO projectDTO, String inputDefinition) {
        projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO,
            "{\"label\": \"Inputs Workflow\", \"inputs\": [" + inputDefinition + "], \"tasks\": []}");

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectDTO.id(), 1)
            .getFirst();
    }

    private long createCronInputProjectDeployment(String cronExpression) {
        return createCronInputProjectDeployment(cronExpression, List.of());
    }

    private long createCronInputProjectDeployment(
        String cronExpression, List<ProjectDeploymentWorkflowConnection> connections) {

        ProjectWorkflow publishedProjectWorkflow = publishCronInputProjectWorkflow(workspace.getId());

        return createCronInputProjectDeployment(
            publishedProjectWorkflow.getProjectId(), publishedProjectWorkflow, cronExpression, connections);
    }

    private long createCronInputProjectDeployment(
        long projectId, ProjectWorkflow projectWorkflow, String cronExpression,
        List<ProjectDeploymentWorkflowConnection> connections) {

        return createCronInputProjectDeployment(projectId, 1, projectWorkflow, cronExpression, connections);
    }

    private long createCronInputProjectDeployment(
        long projectId, int projectVersion, ProjectWorkflow projectWorkflow, String cronExpression,
        List<ProjectDeploymentWorkflowConnection> connections) {

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            connections, null, null, Map.of("cronExpression", cronExpression), true, null, null, null, null, null,
            null, null, 0, projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectId)
            .name("Cron Input Deployment")
            .environment(Environment.DEVELOPMENT)
            .projectVersion(projectVersion)
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        return projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO);
    }

    private ProjectWorkflow publishCronInputProjectWorkflow(long workspaceId) {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspaceId);

        projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO,
            """
                {
                    "label": "Cron Input Workflow",
                    "inputs": [
                        {"name": "cronExpression", "label": "Cron expression", "type": "string", "required": true}
                    ],
                    "triggers": [
                        {
                            "label": "Cron",
                            "name": "trigger_1",
                            "type": "schedule/v1/cron",
                            "parameters": {
                                "expression": "${cronExpression}",
                                "timezone": "Europe/Zagreb"
                            }
                        }
                    ],
                    "tasks": []
                }""");

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        return projectWorkflowRepository.findAllByProjectIdAndProjectVersion(projectDTO.id(), 1)
            .getFirst();
    }

    @Nested
    @ContextConfiguration(classes = PreAuthorizeEnforcement.MethodSecurityConfiguration.class)
    class PreAuthorizeEnforcement {

        private static final long PROJECT_ID = 11L;
        private static final String WORKFLOW_ID = "workflow-41";

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            reset(permissionService);

            authenticate(AuthorityConstants.USER);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testEveryOverloadChecksDeploymentPush() {
            ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
                .projectId(PROJECT_ID)
                .projectVersion(1)
                .build();

            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setProjectId(PROJECT_ID);

            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO));
            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.createProjectDeployment(projectDeployment, WORKFLOW_ID, List.of()));
            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.createProjectDeployment(projectDeployment, List.of(), List.of()));
            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.updateProjectDeployment(projectDeploymentDTO));
            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.updateProjectDeployment(PROJECT_ID, 1, "uuid", List.of(), null));
            assertRequiresScope(
                PROJECT_ID, "Project", "DEPLOYMENT_PUSH",
                () -> projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(), List.of()));

            assertThat(countOverloads(ProjectDeploymentFacadeImpl.class, "createProjectDeployment")).isEqualTo(3);
            assertThat(countOverloads(ProjectDeploymentFacadeImpl.class, "updateProjectDeployment")).isEqualTo(3);
        }

        private void
            assertRequiresScope(Serializable targetId, String targetType, String scope, ThrowingCallable call) {
            reset(permissionService);

            assertDenied(call);

            when(permissionService.hasResourceScope(targetId, targetType, scope)).thenReturn(true);

            assertNotDenied(call);

            reset(permissionService);
        }

        private static void assertDenied(ThrowingCallable call) {
            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        private static void assertNotDenied(ThrowingCallable call) {
            Throwable throwable = catchThrowable(call);

            assertThat(throwable)
                .as("Expected the gate to let the call through, but it was denied: %s", throwable)
                .satisfiesAnyOf(
                    actual -> assertThat(actual).isNull(),
                    actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
        }

        private static void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        private static long countOverloads(Class<?> clazz, String methodName) {
            Method[] methods = clazz.getDeclaredMethods();

            return Arrays.stream(methods)
                .filter(method -> !method.isSynthetic())
                .filter(method -> methodName.equals(method.getName()))
                .count();
        }

        @EnableMethodSecurity
        static class MethodSecurityConfiguration {
        }
    }
}
