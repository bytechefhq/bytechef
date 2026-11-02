/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacadeImpl;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.automation.task.service.ApprovalTaskServiceImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.StringToTypedValueConverter;
import com.bytechef.commons.data.jdbc.converter.TypedValueToStringConverter;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.automation.task.remote.grpc.ApprovalTaskFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.automation.task.remote.grpc.ApprovalTaskProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RemoteApprovalTaskFacadeClientIntTest.RemoteApprovalTaskFacadeClientIntTestConfiguration.class)
class RemoteApprovalTaskFacadeClientIntTest {

    private static final String PUBLIC_URL = "https://example.com";

    @Autowired
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private ManagedChannel managedChannel;
    private RemoteApprovalTaskFacadeClient remoteApprovalTaskFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        jdbcTemplate.update("DELETE FROM approval_task");
        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        ApprovalTaskFacadeImpl approvalTaskFacade = new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, jobService, principalJobService, projectDeploymentService,
            PUBLIC_URL, taskExecutionService, mock(WorkflowService.class));

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ApprovalTaskFacadeRemoteServiceGrpcServer(
                        approvalTaskFacade, new ApprovalTaskProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteApprovalTaskFacadeClient = new RemoteApprovalTaskFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testCreateApprovalTaskStoresItInTheEnvironmentOfItsRun() {
        long jobId = saveJob(Job.Status.STOPPED, null);

        principalJobService.create(jobId, 7L, PlatformType.AUTOMATION);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(projectDeploymentService.getProjectDeployment(7L)).thenReturn(projectDeployment);

        ApprovalTask approvalTask = remoteApprovalTaskFacadeClient.createApprovalTask(
            approvalTask(approvalTokens.toSignedToken(jobResumeId(jobId), Duration.ofHours(1))));

        ApprovalTask storedApprovalTask = approvalTaskService.getApprovalTask(
            Objects.requireNonNull(approvalTask.getId()));

        assertThat(storedApprovalTask.getName()).isEqualTo("Approve the order");
        assertThat(storedApprovalTask.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCreateApprovalTaskWithAForgedTokenIsAnIllegalArgument() {
        assertThatThrownBy(() -> remoteApprovalTaskFacadeClient.createApprovalTask(approvalTask("forged")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(approvalTaskService.countApprovalTasks()).isZero();
    }

    @Test
    void testPendingApprovalsListOnlyRunsWaitingOnAnApprovalInTheRequestedEnvironment() {
        long developmentJobId = saveSuspendedJob(true);

        saveSuspendedJob(false);

        assertThat(remoteApprovalTaskFacadeClient.getPendingApprovals(null))
            .singleElement()
            .satisfies(pendingApproval -> {
                assertThat(pendingApproval.jobId()).isEqualTo(developmentJobId);
                assertThat(pendingApproval.formUrl()).startsWith(PUBLIC_URL + "/resume/");
            });
        assertThat(remoteApprovalTaskFacadeClient.getPendingApprovals(Environment.DEVELOPMENT.ordinal()))
            .extracting(PendingApproval::jobId)
            .containsExactly(developmentJobId);
        assertThat(remoteApprovalTaskFacadeClient.getPendingApprovals(Environment.PRODUCTION.ordinal())).isEmpty();
    }

    private long saveJob(Job.Status status, Map<String, Object> metadata) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        if (metadata != null) {
            job.setMetadata(metadata);
        }

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private long saveSuspendedJob(boolean approvalResume) {
        long jobId = saveJob(Job.Status.STOPPED, null);

        Job job = jobService.getJob(jobId);

        job.setMetadata(
            Map.of(
                MetadataConstants.APPROVAL_RESUME, approvalResume, MetadataConstants.JOB_RESUME_ID,
                jobResumeId(jobId)));

        jobRepository.save(job);

        return jobId;
    }

    private static ApprovalTask approvalTask(String jobResumeId) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(jobResumeId);
        approvalTask.setName("Approve the order");

        return approvalTask;
    }

    private static String jobResumeId(long jobId) {
        JobResumeId jobResumeId = JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId);

        return jobResumeId.toString();
    }

    @EnableAutoConfiguration
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
    })
    @Configuration
    static class RemoteApprovalTaskFacadeClientIntTestConfiguration {

        private static final String SIGNING_SECRET = "approval-token-secret-key-0001!!";

        @Bean
        ApprovalTaskService approvalTaskService(ApprovalTaskRepository approvalTaskRepository) {
            return new ApprovalTaskServiceImpl(approvalTaskRepository);
        }

        @Bean
        ApprovalTokens approvalTokens() {
            byte[] secret = SIGNING_SECRET.getBytes(StandardCharsets.UTF_8);

            return new ApprovalTokensImpl(
                Clock.systemUTC(), EncodingUtils.base64EncodeToString(secret), List.of(), Duration.ofDays(1),
                Duration.ofMinutes(1), false);
        }

        @Bean
        JobService jobService(JobRepository jobRepository) {
            return new JobServiceImpl(jobRepository);
        }

        @Bean
        PrincipalJobService principalJobService(PrincipalJobRepository principalJobRepository) {
            return new PrincipalJobServiceImpl(principalJobRepository);
        }

        @Bean
        TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
            return new TaskExecutionServiceImpl(taskExecutionRepository);
        }

        @Bean
        TenantService tenantService() {
            TenantService tenantService = mock(TenantService.class);

            when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));

            return tenantService;
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class RemoteApprovalTaskFacadeClientIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            @Autowired
            private ObjectMapper objectMapper;

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new ExecutionErrorToStringConverter(objectMapper),
                    new FileEntryToStringConverter(objectMapper),
                    new JobResumeIdToStringConverter(),
                    new MapWrapperToStringConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper),
                    new StringToFileEntryConverter(objectMapper),
                    new StringToJobResumeIdConverter(),
                    new StringToMapWrapperConverter(objectMapper),
                    new StringToTypedValueConverter(objectMapper),
                    new StringToWebhooksConverter(objectMapper),
                    new StringToWorkflowExecutionIdConverter(),
                    new StringToWorkflowTaskConverter(objectMapper),
                    new StringToWorkflowTriggerConverter(objectMapper),
                    new TypedValueToStringConverter(objectMapper),
                    new WebhooksToStringConverter(objectMapper),
                    new WorkflowExecutionIdToStringConverter(),
                    new WorkflowTaskToStringConverter(objectMapper),
                    new WorkflowTriggerToStringConverter(objectMapper));
            }
        }
    }
}
