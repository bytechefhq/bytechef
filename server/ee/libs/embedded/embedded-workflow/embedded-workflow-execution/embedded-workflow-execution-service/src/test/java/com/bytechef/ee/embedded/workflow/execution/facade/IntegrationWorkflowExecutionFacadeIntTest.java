/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.workflow.execution.config.EmbeddedWorkflowExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationWorkflowExecutionFacadeIntTest {

    /**
     * Checks the tenant admin gate with every collaborator of the facade mocked.
     */
    @Nested
    @SpringBootTest(
        classes = MethodSecurityIntTest.Config.class,
        properties = "bytechef.edition=ee")
    @MockitoBean(types = {
        ComponentDefinitionService.class, ContextService.class, EnvironmentService.class,
        Evaluator.class, IntegrationInstanceConfigurationService.class, IntegrationInstanceService.class,
        IntegrationService.class, IntegrationWorkflowFacade.class, IntegrationWorkflowService.class,
        TaskDispatcherDefinitionService.class, TaskFileStorage.class, TriggerExecutionService.class,
        TriggerFileStorage.class, WorkflowService.class
    })
    class MethodSecurityIntTest {

        @Autowired
        private IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

        @MockitoBean
        private JobService jobService;

        @Autowired
        private PermissionService permissionService;

        @MockitoBean
        private PrincipalJobService principalJobService;

        @MockitoBean
        private TaskExecutionService taskExecutionService;

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
        void testNonAdminIsDeniedEveryAdminReadBeforeAnyLookup() {
            when(permissionService.isTenantAdmin()).thenReturn(false);

            assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(1L))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(1L, true))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(1L, 2L, 0, 50))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                    null, null, null, null, null, null, null, null, 0))
                        .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(jobService, principalJobService, taskExecutionService);
        }

        @Test
        void testTenantAdminPassesTheGate() {
            when(permissionService.isTenantAdmin()).thenReturn(true);

            assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(1L))
                .isNotInstanceOf(AccessDeniedException.class);

            verify(jobService).getJob(1L);
        }

        @SpringBootConfiguration
        @EnableMethodSecurity
        @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
        @Import(IntegrationWorkflowExecutionFacadeImpl.class)
        static class Config {

            @Bean("permissionService")
            PermissionService permissionService() {
                return mock(PermissionService.class);
            }
        }
    }

    /**
     * Runs the facade over the real job, task execution, context, principal job, trigger execution, workflow and
     * embedded configuration services backed by PostgreSQL. The component and task dispatcher definition registries and
     * the integration workflow facade, which come from other modules, stay mocked.
     */
    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(
        classes = {
            EmbeddedWorkflowExecutionIntTestConfiguration.class, IntegrationWorkflowExecutionFacadeImpl.class
        },
        properties = {
            "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
            "spring.liquibase.contexts=configuration,execution,user"
        })
    @Import(PostgreSQLContainerConfiguration.class)
    class PersistenceIntTest {

        private static final String TASK_TYPE = "myComponent/v1/myAction";

        @MockitoBean
        private ComponentDefinitionService componentDefinitionService;

        @Autowired
        private ConnectedUserService connectedUserService;

        @Autowired
        private ContextService contextService;

        @Autowired
        private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

        @Autowired
        private IntegrationInstanceService integrationInstanceService;

        @Autowired
        private IntegrationService integrationService;

        @Autowired
        private IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

        @MockitoBean
        private IntegrationWorkflowFacade integrationWorkflowFacade;

        @Autowired
        private IntegrationWorkflowRepository integrationWorkflowRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private JobService jobService;

        @Autowired
        private PrincipalJobService principalJobService;

        @MockitoBean
        private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

        @Autowired
        private TaskExecutionService taskExecutionService;

        @Autowired
        private TaskFileStorage taskFileStorage;

        @Autowired
        private WorkflowService workflowService;

        @BeforeEach
        void beforeEach() {
            ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

            when(componentDefinition.getTitle()).thenReturn("Title");
            when(componentDefinition.getIcon()).thenReturn("icon");
            when(componentDefinitionService.hasComponentDefinition(anyString(), any()))
                .thenReturn(true);
            when(componentDefinitionService.getComponentDefinition(anyString(), any()))
                .thenReturn(componentDefinition);
        }

        @AfterEach
        void afterEach() {
            jdbcTemplate.update("UPDATE job SET parent_task_execution_id = NULL");
            jdbcTemplate.update("DELETE FROM principal_job");
            jdbcTemplate.update("DELETE FROM context");
            jdbcTemplate.update("DELETE FROM task_execution");
            jdbcTemplate.update("DELETE FROM job");
            jdbcTemplate.update("DELETE FROM integration_instance");
            jdbcTemplate.update("DELETE FROM integration_instance_configuration");
            jdbcTemplate.update("DELETE FROM integration_workflow");
            jdbcTemplate.update("DELETE FROM integration_version");
            jdbcTemplate.update("DELETE FROM integration");
            jdbcTemplate.update("DELETE FROM connected_user");
            jdbcTemplate.update("DELETE FROM workflow");
        }

        @Test
        void testGetWorkflowExecutionsIncludesJobsOfPublishedIntegrationVersion() {
            Integration integration = createIntegration();

            String publishedWorkflowId = createWorkflowId();
            String draftWorkflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, publishedWorkflowId, UUID.randomUUID());
            addIntegrationWorkflow(integration, 2, draftWorkflowId, UUID.randomUUID());

            stubIntegrationWorkflowFacade(publishedWorkflowId, draftWorkflowId);

            IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
                integration);
            IntegrationInstance integrationInstance = createIntegrationInstance(
                "user-1", integrationInstanceConfiguration);

            Job job = createPrincipalJob(publishedWorkflowId, integrationInstance);

            Page<WorkflowExecutionDTO> workflowExecutionPage = integrationWorkflowExecutionFacade
                .getWorkflowExecutions(
                    (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, null, null, null, 0);

            assertThat(workflowExecutionPage.getContent())
                .singleElement()
                .satisfies(workflowExecutionDTO -> {
                    assertThat(workflowExecutionDTO.id()).isEqualTo(job.getId());
                    assertThat(workflowExecutionDTO.integration()
                        .getId()).isEqualTo(integration.getId());
                    assertThat(workflowExecutionDTO.integrationInstance()
                        .getId()).isEqualTo(integrationInstance.getId());
                    assertThat(workflowExecutionDTO.integrationInstanceConfiguration()
                        .getId()).isEqualTo(integrationInstanceConfiguration.getId());
                });
        }

        @Test
        void testGetWorkflowExecutionsFiltersByInstancesOfTheConfiguration() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());

            stubIntegrationWorkflowFacade(workflowId);

            IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
                integration);
            IntegrationInstanceConfiguration otherIntegrationInstanceConfiguration =
                createIntegrationInstanceConfiguration(integration);

            Job job = createPrincipalJob(
                workflowId, createIntegrationInstance("user-1", integrationInstanceConfiguration));

            createPrincipalJob(workflowId, createIntegrationInstance("user-2", otherIntegrationInstanceConfiguration));

            Page<WorkflowExecutionDTO> workflowExecutionPage = integrationWorkflowExecutionFacade
                .getWorkflowExecutions(
                    null, null, null, null, null, integrationInstanceConfiguration.getId(), null, null, 0);

            assertThat(workflowExecutionPage.getContent())
                .extracting(WorkflowExecutionDTO::id)
                .containsExactly(job.getId());
        }

        @Test
        void testGetWorkflowExecutionsFiltersByInstancesOfTheConnectedUser() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());

            stubIntegrationWorkflowFacade(workflowId);

            IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
                integration);

            IntegrationInstance connectedUserIntegrationInstance = createIntegrationInstance(
                "user-1", integrationInstanceConfiguration);

            Job job = createPrincipalJob(workflowId, connectedUserIntegrationInstance);

            createPrincipalJob(workflowId, createIntegrationInstance("user-2", integrationInstanceConfiguration));

            Page<WorkflowExecutionDTO> workflowExecutionPage = integrationWorkflowExecutionFacade
                .getWorkflowExecutions(
                    null, null, null, null, null, integrationInstanceConfiguration.getId(),
                    connectedUserIntegrationInstance.getConnectedUserId(), null, 0);

            assertThat(workflowExecutionPage.getContent())
                .extracting(WorkflowExecutionDTO::id)
                .containsExactly(job.getId());
        }

        @Test
        void testGetWorkflowExecutionsReturnsEmptyPageWhenTheConnectedUserHasNoInstances() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());

            stubIntegrationWorkflowFacade(workflowId);

            IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
                integration);

            createPrincipalJob(workflowId, createIntegrationInstance("user-2", integrationInstanceConfiguration));

            long connectedUserId = Objects.requireNonNull(
                connectedUserService.createConnectedUser("user-1", Environment.DEVELOPMENT)
                    .getId());

            Page<WorkflowExecutionDTO> workflowExecutionPage = integrationWorkflowExecutionFacade
                .getWorkflowExecutions(null, null, null, null, null, null, connectedUserId, null, 0);

            assertThat(workflowExecutionPage.getContent()).isEmpty();
        }

        @Test
        void testGetWorkflowExecutionsExpandsWorkflowFilterToEveryIntegrationVersion() {
            Integration integration = createIntegration();

            String draftWorkflowId = createWorkflowId();
            String publishedWorkflowId = createWorkflowId();
            String otherWorkflowId = createWorkflowId();

            UUID workflowUuid = UUID.randomUUID();

            addIntegrationWorkflow(integration, 2, publishedWorkflowId, workflowUuid);
            addIntegrationWorkflow(integration, 1, draftWorkflowId, workflowUuid);
            addIntegrationWorkflow(integration, 1, otherWorkflowId, UUID.randomUUID());

            IntegrationInstance integrationInstance = createIntegrationInstance(
                "user-1", createIntegrationInstanceConfiguration(integration));

            Job publishedJob = createPrincipalJob(publishedWorkflowId, integrationInstance);

            createPrincipalJob(otherWorkflowId, integrationInstance);

            Page<WorkflowExecutionDTO> workflowExecutionPage = integrationWorkflowExecutionFacade
                .getWorkflowExecutions(null, null, null, null, null, null, null, draftWorkflowId, 0);

            assertThat(workflowExecutionPage.getContent())
                .extracting(WorkflowExecutionDTO::id)
                .containsExactly(publishedJob.getId());

            verifyNoInteractions(integrationWorkflowFacade);
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionLoadsTaskData() {
            Job job = createJob("workflow", null);

            TaskExecution taskExecution = createTaskExecutionWithData(job, null);

            TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                Objects.requireNonNull(job.getId()), Objects.requireNonNull(taskExecution.getId()));

            assertThat(taskExecutionDTO.input()).isEqualTo(Map.of("greeting", "Hello Ada"));
            assertThat(taskExecutionDTO.output()).isEqualTo("output-value");
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionAcceptsTaskInDescendantJob() {
            Job rootJob = createJob("workflow", null);

            TaskExecution parentTaskExecution = createTaskExecution(rootJob, null);

            Job childJob = createJob("subflow-workflow", parentTaskExecution.getId());

            TaskExecution taskExecution = createTaskExecutionWithData(childJob, null);

            TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                Objects.requireNonNull(rootJob.getId()), Objects.requireNonNull(taskExecution.getId()));

            assertThat(taskExecutionDTO.input()).isEqualTo(Map.of("greeting", "Hello Ada"));
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionRejectsTaskFromAnotherWorkflowExecution() {
            Job job = createJob("workflow", null);
            Job otherJob = createJob("workflow", null);

            TaskExecution taskExecution = createTaskExecutionWithData(job, null);

            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                    Objects.requireNonNull(otherJob.getId()), Objects.requireNonNull(taskExecution.getId())))
                        .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionReportsMissingTaskExecution() {
            Job job = createJob("workflow", null);

            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                    Objects.requireNonNull(job.getId()), Long.MAX_VALUE))
                        .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionAttachesShallowChildJob() {
            Job job = createJob("workflow", null);

            TaskExecution taskExecution = createTaskExecutionWithData(job, null);

            Job childJob = new Job();

            childJob.setInputs(Map.of("input", "value"));
            childJob.setLabel("Subflow");
            childJob.setParentTaskExecutionId(taskExecution.getId());
            childJob.setStatus(Job.Status.COMPLETED);
            childJob.setWorkflowId("subflow-workflow");

            childJob = jobService.update(childJob);

            createTaskExecution(childJob, null);

            TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                Objects.requireNonNull(job.getId()), Objects.requireNonNull(taskExecution.getId()));

            JobDTO childJobDTO = Objects.requireNonNull(taskExecutionDTO.childJob());

            assertThat(childJobDTO.id()).isEqualTo(childJob.getId());
            assertThat(childJobDTO.label()).isEqualTo("Subflow");
            assertThat(childJobDTO.status()).isEqualTo(Job.Status.COMPLETED);
            assertThat(childJobDTO.inputs()).isEmpty();
            assertThat(childJobDTO.taskExecutions()).isEmpty();
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionChildrenAcceptsTaskInDescendantJob() {
            Job rootJob = createJob("workflow", null);

            TaskExecution parentTaskExecution = createTaskExecution(rootJob, null);

            Job childJob = createJob("subflow-workflow", parentTaskExecution.getId());

            TaskExecution taskExecution = createTaskExecution(childJob, null);

            createTaskExecution(childJob, taskExecution.getId());

            TaskExecutionChildrenPageDTO taskExecutionChildrenPageDTO = integrationWorkflowExecutionFacade
                .getWorkflowExecutionTaskExecutionChildren(
                    Objects.requireNonNull(rootJob.getId()), Objects.requireNonNull(taskExecution.getId()), 0, 50);

            assertThat(taskExecutionChildrenPageDTO.number()).isZero();
            assertThat(taskExecutionChildrenPageDTO.size()).isEqualTo(50);
            assertThat(taskExecutionChildrenPageDTO.totalElements()).isEqualTo(1L);
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionChildrenRejectsTaskFromAnotherWorkflowExecution() {
            Job job = createJob("workflow", null);
            Job otherJob = createJob("workflow", null);

            TaskExecution taskExecution = createTaskExecution(job, null);

            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(
                    Objects.requireNonNull(otherJob.getId()), Objects.requireNonNull(taskExecution.getId()), 0, 50))
                        .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testGetWorkflowExecutionLazyReturnsTopLevelTaskExecutionsWithFocusPathAndCount() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());

            IntegrationInstance integrationInstance = createIntegrationInstance(
                "user-1", createIntegrationInstanceConfiguration(integration));

            Job job = createPrincipalJob(workflowId, integrationInstance);

            TaskExecution topLevelTaskExecution = createTaskExecution(job, null);

            createTaskExecution(job, topLevelTaskExecution.getId());

            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
                Objects.requireNonNull(job.getId()), true);

            assertThat(workflowExecutionDTO.integrationInstance()
                .getId()).isEqualTo(integrationInstance.getId());
            assertThat(workflowExecutionDTO.focusPath())
                .first()
                .isEqualTo(new FocusPathElementDTO(Objects.requireNonNull(topLevelTaskExecution.getId()), null, null));
            assertThat(workflowExecutionDTO.taskExecutionCount()).isEqualTo(2L);
            assertThat(workflowExecutionDTO.job()
                .taskExecutions())
                    .extracting(TaskExecutionDTO::id, TaskExecutionDTO::childCount)
                    .containsExactly(tuple(topLevelTaskExecution.getId(), 1));
        }

        @Test
        void testGetWorkflowExecutionIsNotLazyByDefault() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());

            Job job = createPrincipalJob(
                workflowId, createIntegrationInstance("user-1", createIntegrationInstanceConfiguration(integration)));

            TaskExecution topLevelTaskExecution = createTaskExecution(job, null);

            createTaskExecution(job, topLevelTaskExecution.getId());

            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
                Objects.requireNonNull(job.getId()));

            assertThat(workflowExecutionDTO.focusPath()).isNull();
            assertThat(workflowExecutionDTO.taskExecutionCount()).isNull();
            assertThat(workflowExecutionDTO.job()
                .taskExecutions())
                    .singleElement()
                    .satisfies(taskExecutionDTO -> assertThat(taskExecutionDTO.children()).hasSize(1));
        }

        @Test
        void testGetWorkflowExecutionResolvesSubflowIntegrationInstanceThroughTheRootJob() {
            Integration integration = createIntegration();

            String workflowId = createWorkflowId();
            String subflowWorkflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());
            addIntegrationWorkflow(integration, 1, subflowWorkflowId, UUID.randomUUID());

            IntegrationInstance integrationInstance = createIntegrationInstance(
                "user-1", createIntegrationInstanceConfiguration(integration));

            Job rootJob = createPrincipalJob(workflowId, integrationInstance);

            TaskExecution parentTaskExecution = createTaskExecution(rootJob, null);

            Job childJob = createJob(subflowWorkflowId, parentTaskExecution.getId());

            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
                Objects.requireNonNull(childJob.getId()), true);

            assertThat(workflowExecutionDTO.integrationInstance()
                .getId()).isEqualTo(integrationInstance.getId());
            assertThat(workflowExecutionDTO.integrationInstanceConfiguration()).isNotNull();
            assertThat(workflowExecutionDTO.triggerExecution()).isNull();
        }

        @Test
        void testGetWorkflowExecutionOmitsTheRootInstanceOfASubflowFromAnotherIntegration() {
            Integration integration = createIntegration();
            Integration subflowIntegration = createIntegration();

            String workflowId = createWorkflowId();
            String subflowWorkflowId = createWorkflowId();

            addIntegrationWorkflow(integration, 1, workflowId, UUID.randomUUID());
            addIntegrationWorkflow(subflowIntegration, 1, subflowWorkflowId, UUID.randomUUID());

            Job rootJob = createPrincipalJob(
                workflowId, createIntegrationInstance("user-1", createIntegrationInstanceConfiguration(integration)));

            TaskExecution parentTaskExecution = createTaskExecution(rootJob, null);

            Job childJob = createJob(subflowWorkflowId, parentTaskExecution.getId());

            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
                Objects.requireNonNull(childJob.getId()), true);

            assertThat(workflowExecutionDTO.integration()
                .getId()).isEqualTo(subflowIntegration.getId());
            assertThat(workflowExecutionDTO.integrationInstance()).isNull();
            assertThat(workflowExecutionDTO.integrationInstanceConfiguration()).isNull();
        }

        @Test
        void testGetWorkflowExecutionFailsOnACyclicSubflowJobChain() {
            Job childJob = createCyclicSubflowJobChain();

            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecution(
                    Objects.requireNonNull(childJob.getId()), true))
                        .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void testGetWorkflowExecutionTaskExecutionFailsOnACyclicSubflowJobChain() {
            Job childJob = createCyclicSubflowJobChain();
            Job unrelatedJob = createJob("workflow", null);

            TaskExecution taskExecution = createTaskExecution(childJob, null);

            assertThatThrownBy(
                () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                    Objects.requireNonNull(unrelatedJob.getId()), Objects.requireNonNull(taskExecution.getId())))
                        .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void testToTaskExecutionDTODoesNotLoadTaskDataForList() {
            Job job = createJob("workflow", null);

            TaskExecution taskExecution = taskExecutionService.create(
                TaskExecution.builder()
                    .jobId(job.getId())
                    .output(
                        taskFileStorage.storeTaskExecutionOutput(
                            Objects.requireNonNull(job.getId()), 0L, "output-value"))
                    .status(TaskExecution.Status.COMPLETED)
                    .taskNumber(0)
                    .workflowTask(createWorkflowTask())
                    .build());

            IntegrationWorkflowExecutionFacadeImpl integrationWorkflowExecutionFacadeImpl =
                AopTestUtils.getTargetObject(integrationWorkflowExecutionFacade);

            TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacadeImpl.toTaskExecutionDTO(
                taskExecution, null, false);

            assertThat(taskExecutionDTO.input()).isNull();
            assertThat(taskExecutionDTO.output()).isNull();
        }

        private void addIntegrationWorkflow(
            Integration integration, int integrationVersion, String workflowId, UUID uuid) {

            integrationWorkflowRepository.save(
                new IntegrationWorkflow(
                    Objects.requireNonNull(integration.getId()), integrationVersion, workflowId, uuid));
        }

        private Job createCyclicSubflowJobChain() {
            Job childJob = createJob("subflow-workflow", null);
            Job parentJob = createJob("workflow", null);

            TaskExecution childJobTaskExecution = createTaskExecution(childJob, null);
            TaskExecution parentJobTaskExecution = createTaskExecution(parentJob, null);

            childJob.setParentTaskExecutionId(parentJobTaskExecution.getId());

            childJob = jobService.update(childJob);

            parentJob.setParentTaskExecutionId(childJobTaskExecution.getId());

            jobService.update(parentJob);

            return childJob;
        }

        private Integration createIntegration() {
            Integration integration = new Integration();

            integration.setComponentName("slack");
            integration.setName("Slack " + UUID.randomUUID());

            return integrationService.create(integration);
        }

        private IntegrationInstance createIntegrationInstance(
            String externalUserId, IntegrationInstanceConfiguration integrationInstanceConfiguration) {

            long connectedUserId = connectedUserService.fetchConnectedUser(externalUserId, Environment.DEVELOPMENT)
                .orElseGet(() -> connectedUserService.createConnectedUser(externalUserId, Environment.DEVELOPMENT))
                .getId();

            return integrationInstanceService.create(
                connectedUserId, 1L, Objects.requireNonNull(integrationInstanceConfiguration.getId()));
        }

        private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(Integration integration) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

            integrationInstanceConfiguration.setConnectionParameters(Map.of());
            integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
            integrationInstanceConfiguration.setIntegrationId(integration.getId());
            integrationInstanceConfiguration.setIntegrationVersion(1);
            integrationInstanceConfiguration.setName("Configuration " + UUID.randomUUID());

            return integrationInstanceConfigurationService.create(integrationInstanceConfiguration);
        }

        private Job createJob(String workflowId, Long parentTaskExecutionId) {
            Job job = new Job();

            job.setParentTaskExecutionId(parentTaskExecutionId);
            job.setStatus(Job.Status.COMPLETED);
            job.setWorkflowId(workflowId);

            return jobService.update(job);
        }

        private Job createPrincipalJob(String workflowId, IntegrationInstance integrationInstance) {
            Job job = createJob(workflowId, null);

            principalJobService.create(
                Objects.requireNonNull(job.getId()), Objects.requireNonNull(integrationInstance.getId()),
                PlatformType.EMBEDDED);

            return job;
        }

        private TaskExecution createTaskExecution(Job job, Long parentId) {
            return taskExecutionService.create(
                TaskExecution.builder()
                    .jobId(job.getId())
                    .parentId(parentId)
                    .status(TaskExecution.Status.COMPLETED)
                    .taskNumber(0)
                    .workflowTask(createWorkflowTask())
                    .build());
        }

        private TaskExecution createTaskExecutionWithData(Job job, Long parentId) {
            long jobId = Objects.requireNonNull(job.getId());

            FileEntry outputFileEntry = taskFileStorage.storeTaskExecutionOutput(jobId, 0L, "output-value");

            TaskExecution taskExecution = taskExecutionService.create(
                TaskExecution.builder()
                    .jobId(jobId)
                    .output(outputFileEntry)
                    .parentId(parentId)
                    .status(TaskExecution.Status.COMPLETED)
                    .taskNumber(0)
                    .workflowTask(createWorkflowTask())
                    .build());

            long taskExecutionId = Objects.requireNonNull(taskExecution.getId());

            contextService.push(
                taskExecutionId, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    taskExecutionId, Context.Classname.TASK_EXECUTION, Map.of("name", "Ada")));

            return taskExecution;
        }

        private static WorkflowTask createWorkflowTask() {
            return new WorkflowTask(
                Map.of(
                    "name", "myAction_1", "type", TASK_TYPE, "parameters",
                    Map.of("greeting", "='Hello ' + name")));
        }

        private String createWorkflowId() {
            Workflow workflow = workflowService.create(
                "{\"label\": \"Workflow\", \"tasks\": []}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

            return Objects.requireNonNull(workflow.getId());
        }

        private void stubIntegrationWorkflowFacade(String... workflowIds) {
            List<IntegrationWorkflowDTO> integrationWorkflowDTOs = Arrays.stream(workflowIds)
                .map(workflowId -> {
                    IntegrationWorkflowDTO integrationWorkflowDTO = mock(IntegrationWorkflowDTO.class);

                    when(integrationWorkflowDTO.getId()).thenReturn(workflowId);

                    return integrationWorkflowDTO;
                })
                .toList();

            when(integrationWorkflowFacade.getIntegrationWorkflows()).thenReturn(integrationWorkflowDTOs);
        }
    }
}
