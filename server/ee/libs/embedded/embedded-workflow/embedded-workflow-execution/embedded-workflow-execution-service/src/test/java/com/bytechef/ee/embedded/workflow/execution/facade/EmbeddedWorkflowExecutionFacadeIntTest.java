/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
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
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.workflow.execution.config.EmbeddedWorkflowExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.workflow.execution.dto.EmbeddedWorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.dto.EmbeddedWorkflowExecutionDTO.Source;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO.Kind;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionScopeDTO;
import com.bytechef.platform.workflow.execution.repository.WorkflowExecutionRowRepositoryImpl;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedWorkflowExecutionIntTestConfiguration.class, EmbeddedWorkflowExecutionFacadeImpl.class,
        IntegrationWorkflowExecutionFacadeImpl.class, WorkflowExecutionRowRepositoryImpl.class,
        WorkflowExecutionRowServiceImpl.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,execution,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
class EmbeddedWorkflowExecutionFacadeIntTest {

    private static final long AUTOMATION_PROJECT_DEPLOYMENT_ID = 500L;
    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedWorkflowExecutionFacade embeddedWorkflowExecutionFacade;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationService integrationService;

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
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @MockitoBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn("Title");
        when(componentDefinitionService.hasComponentDefinition(anyString(), any())).thenReturn(true);
        when(componentDefinitionService.getComponentDefinition(anyString(), any())).thenReturn(componentDefinition);
        when(projectWorkflowExecutionFacade.getWorkflowExecutions(anyList())).thenAnswer(invocation -> {
            List<WorkflowExecutionRowDTO> workflowExecutionRowDTOs = invocation.getArgument(0);

            return workflowExecutionRowDTOs.stream()
                .map(EmbeddedWorkflowExecutionFacadeIntTest::createAutomationWorkflowExecutionDTO)
                .toList();
        });
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM principal_job");
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
    void testGetWorkflowExecutionsPagesIntegrationAndAutomationRunsTogetherNewestFirst() {
        IntegrationInstance integrationInstance = createIntegrationInstance();
        String integrationWorkflowId = getIntegrationWorkflowId(integrationInstance);
        String automationWorkflowId = createWorkflowId();

        Job newestIntegrationJob = createJob(
            integrationWorkflowId, NOW.minus(1, ChronoUnit.HOURS), integrationInstance.getId(), PlatformType.EMBEDDED);
        Job automationJob = createJob(
            automationWorkflowId, NOW.minus(2, ChronoUnit.HOURS), AUTOMATION_PROJECT_DEPLOYMENT_ID,
            PlatformType.AUTOMATION);
        Job oldestIntegrationJob = createJob(
            integrationWorkflowId, NOW.minus(3, ChronoUnit.HOURS), integrationInstance.getId(), PlatformType.EMBEDDED);

        stubAutomationScope(automationWorkflowId);

        Page<EmbeddedWorkflowExecutionDTO> page = embeddedWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, 1L, 0);

        assertThat(page.getContent())
            .extracting(EmbeddedWorkflowExecutionDTO::source, EmbeddedWorkflowExecutionFacadeIntTest::getId)
            .containsExactly(
                tuple(Source.INTEGRATION, newestIntegrationJob.getId()),
                tuple(Source.AUTOMATION, automationJob.getId()),
                tuple(Source.INTEGRATION, oldestIntegrationJob.getId()));
        assertThat(page.getTotalElements()).isEqualTo(3);

        verify(projectWorkflowExecutionFacade).getWorkflowExecutions(
            List.of(new WorkflowExecutionRowDTO(Kind.JOB, Objects.requireNonNull(automationJob.getId()))));
    }

    @Test
    void testGetWorkflowExecutionsReturnsOnlyIntegrationRunsWithoutAnAutomationScope() {
        IntegrationInstance integrationInstance = createIntegrationInstance();

        Job integrationJob = createJob(
            getIntegrationWorkflowId(integrationInstance), NOW, integrationInstance.getId(), PlatformType.EMBEDDED);

        createJob(createWorkflowId(), NOW, AUTOMATION_PROJECT_DEPLOYMENT_ID, PlatformType.AUTOMATION);

        Page<EmbeddedWorkflowExecutionDTO> page = embeddedWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, 1L, 0);

        assertThat(page.getContent())
            .extracting(EmbeddedWorkflowExecutionDTO::source, EmbeddedWorkflowExecutionFacadeIntTest::getId)
            .containsExactly(tuple(Source.INTEGRATION, integrationJob.getId()));
    }

    @Test
    void testGetWorkflowExecutionsReturnsAnEmptyPageWithoutAnyScope() {
        createJob(createWorkflowId(), NOW, AUTOMATION_PROJECT_DEPLOYMENT_ID, PlatformType.AUTOMATION);

        Page<EmbeddedWorkflowExecutionDTO> page = embeddedWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, 1L, 0);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void testGetWorkflowExecutionsPassesTheConnectedUserToBothScopes() {
        IntegrationInstance integrationInstance = createIntegrationInstance();
        IntegrationInstance otherIntegrationInstance = createIntegrationInstance();

        Job integrationJob = createJob(
            getIntegrationWorkflowId(integrationInstance), NOW, integrationInstance.getId(), PlatformType.EMBEDDED);

        createJob(
            getIntegrationWorkflowId(otherIntegrationInstance), NOW, otherIntegrationInstance.getId(),
            PlatformType.EMBEDDED);

        Page<EmbeddedWorkflowExecutionDTO> page = embeddedWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, integrationInstance.getConnectedUserId(), 1L,
            0);

        assertThat(page.getContent())
            .extracting(EmbeddedWorkflowExecutionFacadeIntTest::getId)
            .containsExactly(integrationJob.getId());

        verify(projectWorkflowExecutionFacade).getEmbeddedWorkflowExecutionScope(
            (long) Environment.DEVELOPMENT.ordinal(), null, integrationInstance.getConnectedUserId(), 1L);
    }

    private static com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO
        createAutomationWorkflowExecutionDTO(WorkflowExecutionRowDTO workflowExecutionRowDTO) {

        com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO workflowExecutionDTO = mock(
            com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO.class);

        when(workflowExecutionDTO.id()).thenReturn(workflowExecutionRowDTO.id());
        when(workflowExecutionDTO.job()).thenReturn(mock(JobDTO.class));

        return workflowExecutionDTO;
    }

    private static Long getId(EmbeddedWorkflowExecutionDTO embeddedWorkflowExecutionDTO) {
        if (embeddedWorkflowExecutionDTO.source() == Source.AUTOMATION) {
            return Objects.requireNonNull(embeddedWorkflowExecutionDTO.automationWorkflowExecution())
                .id();
        }

        return Objects.requireNonNull(embeddedWorkflowExecutionDTO.integrationWorkflowExecution())
            .id();
    }

    private IntegrationInstance createIntegrationInstance() {
        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(integration);

        String workflowId = createWorkflowId();

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(Objects.requireNonNull(integration.getId()), 1, workflowId, UUID.randomUUID()));

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = new ArrayList<>(
            integrationWorkflowFacade.getIntegrationWorkflows());

        IntegrationWorkflowDTO integrationWorkflowDTO = mock(IntegrationWorkflowDTO.class);

        when(integrationWorkflowDTO.getId()).thenReturn(workflowId);

        integrationWorkflowDTOs.add(integrationWorkflowDTO);

        when(integrationWorkflowFacade.getIntegrationWorkflows()).thenReturn(integrationWorkflowDTOs);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Configuration " + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        String externalUserId = "user-" + UUID.randomUUID();

        long connectedUserId = connectedUserService.createConnectedUser(externalUserId, Environment.DEVELOPMENT)
            .getId();

        return integrationInstanceService.create(
            connectedUserId, 1L, Objects.requireNonNull(integrationInstanceConfiguration.getId()));
    }

    private Job createJob(String workflowId, Instant startDate, long principalId, PlatformType platformType) {
        Job job = new Job();

        job.setStartDate(startDate);
        job.setStatus(Job.Status.COMPLETED);
        job.setWorkflowId(workflowId);

        job = jobService.update(job);

        principalJobService.create(Objects.requireNonNull(job.getId()), principalId, platformType);

        return job;
    }

    private String createWorkflowId() {
        Workflow workflow = workflowService.create(
            "{\"label\": \"Workflow\", \"tasks\": []}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return Objects.requireNonNull(workflow.getId());
    }

    private String getIntegrationWorkflowId(IntegrationInstance integrationInstance) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstance.getIntegrationInstanceConfigurationId());

        return integrationWorkflowRepository.findAllByIntegrationId(integrationInstanceConfiguration.getIntegrationId())
            .getFirst()
            .getWorkflowId();
    }

    private void stubAutomationScope(String automationWorkflowId) {
        when(projectWorkflowExecutionFacade.getEmbeddedWorkflowExecutionScope(any(), any(), any(), anyLong()))
            .thenReturn(
                new WorkflowExecutionScopeDTO(
                    PlatformType.AUTOMATION, List.of(AUTOMATION_PROJECT_DEPLOYMENT_ID), List.of(automationWorkflowId),
                    List.of()));
    }
}
