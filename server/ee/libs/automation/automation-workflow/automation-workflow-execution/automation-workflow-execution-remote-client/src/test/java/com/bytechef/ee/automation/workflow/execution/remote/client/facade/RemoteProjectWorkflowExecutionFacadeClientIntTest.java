/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacadeImpl;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.ProjectWorkflowExecutionRemoteServiceGrpcServer;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.WorkflowExecutionProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.repository.WorkflowExecutionRowRepositoryImpl;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTaskStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TaskStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RemoteProjectWorkflowExecutionFacadeClientIntTest.RemoteProjectWorkflowExecutionFacadeClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
class RemoteProjectWorkflowExecutionFacadeClientIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {
          "label": "Remote Workflow",
          "tasks": [
            {"name": "task", "type": "myComponent/v1/myAction", "parameters": {"evaluated": true}}
          ]
        }
        """;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds;

    @Autowired
    private ProjectWorkflowExecutionFacade facade;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @MockitoBean
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @MockitoBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @MockitoBean
    private TriggerFileStorage triggerFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ManagedChannel managedChannel;
    private RemoteProjectWorkflowExecutionFacadeClient remoteProjectWorkflowExecutionFacadeClient;
    private Server server;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() throws IOException {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn("Title");
        when(componentDefinition.getIcon()).thenReturn("icon");
        when(componentDefinitionService.hasComponentDefinition(anyString(), any())).thenReturn(true);
        when(componentDefinitionService.getComponentDefinition(anyString(), any())).thenReturn(componentDefinition);
        when(projectFacade.getWorkspaceProjectWorkflows(anyLong())).thenReturn(List.of());

        workspace = workspaceRepository.save(new Workspace("remote-execution-workspace-" + UUID.randomUUID()));

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ProjectWorkflowExecutionRemoteServiceGrpcServer(
                        facade, new WorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectWorkflowExecutionFacadeClient = new RemoteProjectWorkflowExecutionFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        embeddedProjectDeploymentIds.clear();
    }

    @Test
    void testGetWorkflowExecutionReturnsTheExecutionOfTheServingApp() {
        Project project = createProject();

        String workflowId = createWorkflowId(project);

        Job job = saveJob(workflowId);

        TaskExecution taskExecution = saveTaskExecution(job);

        ProjectDeployment projectDeployment = createProjectDeployment(project);

        principalJobService.create(getId(job), getId(projectDeployment), PlatformType.AUTOMATION);

        WorkflowExecutionDTO workflowExecutionDTO =
            remoteProjectWorkflowExecutionFacadeClient.getWorkflowExecution(getId(job), false);

        assertThat(workflowExecutionDTO.id()).isEqualTo(job.getId());
        assertThat(workflowExecutionDTO.project()
            .getName()).isEqualTo(project.getName());
        assertThat(workflowExecutionDTO.projectDeployment()
            .getId()).isEqualTo(projectDeployment.getId());
        assertThat(workflowExecutionDTO.workflow()
            .getLabel()).isEqualTo("Remote Workflow");

        JobDTO jobDTO = Objects.requireNonNull(workflowExecutionDTO.job());

        assertThat(jobDTO.id()).isEqualTo(job.getId());
        assertThat(jobDTO.status()).isEqualTo(Job.Status.STARTED);
        assertThat(jobDTO.taskExecutions())
            .extracting(TaskExecutionDTO::id)
            .containsExactly(taskExecution.getId());
    }

    @Test
    void testGetWorkflowExecutionsPagesTheWorkspacesExecutions() {
        Project project = createProject();

        Job job = saveJob(createWorkflowId(project));

        ProjectDeployment projectDeployment = createProjectDeployment(project);

        principalJobService.create(getId(job), getId(projectDeployment), PlatformType.AUTOMATION);

        Page<WorkflowExecutionDTO> page = remoteProjectWorkflowExecutionFacadeClient.getWorkflowExecutions(
            false, null, Job.Status.STARTED, null, null, getId(project), null, null, null, null, getId(workspace), 0);

        assertThat(page.getContent())
            .extracting(WorkflowExecutionDTO::id)
            .containsExactly(job.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    void testGetWorkflowExecutionOfAnUnknownJobFails() {
        assertThatThrownBy(() -> remoteProjectWorkflowExecutionFacadeClient.getWorkflowExecution(Long.MAX_VALUE, false))
            .isInstanceOf(StatusRuntimeException.class);
    }

    private Project createProject() {
        Project project = Project.builder()
            .name("project-" + UUID.randomUUID())
            .workspaceId(workspace.getId())
            .build();

        return projectService.create(project);
    }

    private ProjectDeployment createProjectDeployment(Project project) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("remote");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        return projectDeploymentService.create(projectDeployment);
    }

    private String createWorkflowId(Project project) {
        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String workflowId = Objects.requireNonNull(workflow.getId());

        projectWorkflowRepository.save(new ProjectWorkflow(getId(project), 1, workflowId, UUID.randomUUID()));

        return workflowId;
    }

    private static long getId(Job job) {
        return Objects.requireNonNull(job.getId());
    }

    private static long getId(Project project) {
        return Objects.requireNonNull(project.getId());
    }

    private static long getId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getId());
    }

    private static long getId(Workspace workspace) {
        return Objects.requireNonNull(workspace.getId());
    }

    private Job saveJob(String workflowId) {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId(workflowId);

        return jobRepository.save(job);
    }

    private TaskExecution saveTaskExecution(Job job) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(job.getId())
            .status(TaskExecution.Status.COMPLETED)
            .workflowTask(
                new WorkflowTask(
                    Map.of("name", "task", "type", "myComponent/v1/myAction", "parameters", Map.of("evaluated", true))))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }

    static class EmbeddedProjectDeploymentIds {

        private final Set<Long> projectDeploymentIds = ConcurrentHashMap.newKeySet();

        void add(long projectDeploymentId) {
            projectDeploymentIds.add(projectDeploymentId);
        }

        void clear() {
            projectDeploymentIds.clear();
        }

        String getConditionTemplate() {
            if (projectDeploymentIds.isEmpty()) {
                return "%1$s = 0";
            }

            return "%1$s IN (" + projectDeploymentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + ")";
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        CacheConfiguration.class, ContextServiceImpl.class, EnvironmentServiceImpl.class, JacksonConfiguration.class,
        JobServiceImpl.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
        PrincipalJobServiceImpl.class, ProjectCallback.class, ProjectDeploymentServiceImpl.class,
        ProjectServiceImpl.class,
        ProjectWorkflowCallback.class,
        ProjectWorkflowExecutionFacadeImpl.class, ProjectWorkflowServiceImpl.class,
        TaskExecutionServiceImpl.class, TriggerExecutionServiceImpl.class, WorkflowExecutionRowRepositoryImpl.class,
        WorkflowExecutionRowServiceImpl.class, WorkflowServiceImpl.class
    })
    static class RemoteProjectWorkflowExecutionFacadeClientIntTestConfiguration {

        @Bean
        EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds() {
            return new EmbeddedProjectDeploymentIds();
        }

        @Bean
        ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor(
            EmbeddedProjectDeploymentIds embeddedProjectDeploymentIds) {

            return ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, embeddedProjectDeploymentIds::getConditionTemplate);
        }

        @Bean
        Evaluator evaluator() {
            return SpelEvaluator.create();
        }

        @Bean
        TaskFileStorage taskFileStorage() {
            return new TaskFileStorageImpl(new Base64FileStorageService());
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class RemoteProjectWorkflowExecutionFacadeClientIntTestJdbcConfiguration
            extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            RemoteProjectWorkflowExecutionFacadeClientIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new ExecutionErrorToStringConverter(objectMapper), new FileEntryToStringConverter(objectMapper),
                    new JobResumeIdToStringConverter(), new MapWrapperToStringConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper), new StringToFileEntryConverter(objectMapper),
                    new StringToJobResumeIdConverter(), new StringToMapWrapperConverter(objectMapper),
                    new StringToTaskStateValueConverter(objectMapper),
                    new StringToTriggerStateValueConverter(objectMapper), new StringToWebhooksConverter(objectMapper),
                    new StringToWorkflowExecutionIdConverter(), new StringToWorkflowTaskConverter(objectMapper),
                    new StringToWorkflowTriggerConverter(objectMapper),
                    new TaskStateValueToStringConverter(objectMapper),
                    new TriggerStateValueToStringConverter(objectMapper), new WebhooksToStringConverter(objectMapper),
                    new WorkflowExecutionIdToStringConverter(), new WorkflowTaskToStringConverter(objectMapper),
                    new WorkflowTriggerToStringConverter(objectMapper));
            }
        }
    }
}
