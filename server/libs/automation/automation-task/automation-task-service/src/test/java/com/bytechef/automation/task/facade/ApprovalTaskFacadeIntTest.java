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

package com.bytechef.automation.task.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.config.ApprovalTaskMethodSecurityTestConfiguration;
import com.bytechef.automation.task.config.ApprovalTaskMethodSecurityTestConfiguration.AuthorizationCheck;
import com.bytechef.automation.task.config.AutomationTaskIntTestConfiguration;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.security.ApprovalTaskJobResolver;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationTaskIntTestConfiguration.class)
class ApprovalTaskFacadeIntTest {

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
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private ApprovalTaskFacade approvalTaskFacade;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM approval_task");
        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        approvalTaskFacade = new ApprovalTaskFacadeImpl(
            approvalTaskService, approvalTokens, jobService, principalJobService, projectDeploymentService,
            PUBLIC_URL, taskExecutionService, workflowService);
    }

    @Test
    void testSignedResumeTokenResolvesTheDeploymentEnvironment() {
        long jobId = saveJob(Job.Status.STOPPED, null);

        deploy(jobId, 7L, Environment.PRODUCTION);

        ApprovalTask approvalTask = approvalTaskFacade.createApprovalTask(
            approvalTask(signedToken(jobId)));

        ApprovalTask storedApprovalTask = approvalTaskService.getApprovalTask(
            Objects.requireNonNull(approvalTask.getId()));

        assertThat(storedApprovalTask.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testRunWithoutDeploymentIsDevelopment() {
        long jobId = saveJob(Job.Status.STOPPED, null);

        ApprovalTask approvalTask = approvalTaskFacade.createApprovalTask(approvalTask(jobResumeId(jobId)));

        ApprovalTask storedApprovalTask = approvalTaskService.getApprovalTask(
            Objects.requireNonNull(approvalTask.getId()));

        assertThat(storedApprovalTask.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testUnresolvableResumeTokenFailsTheCreate() {
        assertThatThrownBy(() -> approvalTaskFacade.createApprovalTask(approvalTask("forged")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(approvalTaskService.countApprovalTasks()).isZero();
    }

    @Test
    void testEnvironmentFilterKeepsOnlyRunsOfThatEnvironment() {
        long productionJobId = saveSuspendedJob(true, null);
        long developmentJobId = saveSuspendedJob(true, null);

        deploy(productionJobId, 11L, Environment.PRODUCTION);

        assertThat(approvalTaskFacade.getPendingApprovals(Environment.PRODUCTION.ordinal()))
            .extracting(PendingApproval::jobId)
            .containsExactly(productionJobId);
        assertThat(approvalTaskFacade.getPendingApprovals(Environment.DEVELOPMENT.ordinal()))
            .extracting(PendingApproval::jobId)
            .containsExactly(developmentJobId);
    }

    @Test
    void testOnlyRunsWaitingOnAnApprovalAreListed() {
        long approvalJobId = saveSuspendedJob(true, null);

        saveSuspendedJob(false, null);
        saveJob(Job.Status.STARTED, Map.of(MetadataConstants.APPROVAL_RESUME, true));

        assertThat(approvalTaskFacade.getPendingApprovals(null))
            .extracting(PendingApproval::jobId)
            .containsExactly(approvalJobId);
    }

    @Test
    void testPendingApprovalCarriesItsExpiryAndAFormLinkForItsRun() {
        Instant expiresAt = Instant.now()
            .plus(Duration.ofDays(2))
            .truncatedTo(ChronoUnit.SECONDS);

        long jobId = saveSuspendedJob(true, expiresAt);

        List<PendingApproval> pendingApprovals = approvalTaskFacade.getPendingApprovals(null);

        assertThat(pendingApprovals).hasSize(1);

        PendingApproval pendingApproval = pendingApprovals.getFirst();

        assertThat(pendingApproval.expiresAt()).isEqualTo(expiresAt);
        assertThat(pendingApproval.formUrl()).startsWith(PUBLIC_URL + "/resume/");

        String formUrl = Objects.requireNonNull(pendingApproval.formUrl());

        String token = formUrl.substring((PUBLIC_URL + "/resume/").length());

        assertThat(approvalTokens.resolveJobResumeId(token))
            .map(JobResumeId::getJobId)
            .contains(jobId);
    }

    @Test
    void testPendingApprovalIsLabelledWithItsWorkflow() {
        long jobId = saveSuspendedJob(true, null);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Expense approval");
        when(workflowService.getWorkflow("demo:1234")).thenReturn(workflow);

        assertThat(approvalTaskFacade.getPendingApprovals(null))
            .singleElement()
            .satisfies(pendingApproval -> {
                assertThat(pendingApproval.jobId()).isEqualTo(jobId);
                assertThat(pendingApproval.workflowLabel()).isEqualTo("Expense approval");
            });
    }

    private void deploy(long jobId, long projectDeploymentId, Environment environment) {
        principalJobService.create(jobId, projectDeploymentId, PlatformType.AUTOMATION);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);

        when(projectDeploymentService.getProjectDeployment(projectDeploymentId)).thenReturn(projectDeployment);
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

    private long saveSuspendedJob(boolean approvalResume, Instant expiresAt) {
        long jobId = saveJob(Job.Status.STOPPED, null);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.APPROVAL_RESUME, approvalResume);
        metadata.put(MetadataConstants.JOB_RESUME_ID, jobResumeId(jobId));

        if (expiresAt != null) {
            TaskExecution taskExecution = TaskExecution.builder()
                .jobId(jobId)
                .taskNumber(1)
                .workflowTask(new WorkflowTask(Map.of("name", "approval1", "type", "approval/v1/approve")))
                .build();

            taskExecution.putMetadata(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toString()));

            taskExecution = taskExecutionRepository.save(taskExecution);

            metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, Objects.requireNonNull(taskExecution.getId()));
        }

        Job job = jobService.getJob(jobId);

        job.setMetadata(metadata);

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

    private String signedToken(long jobId) {
        return approvalTokens.toSignedToken(jobResumeId(jobId), Duration.ofHours(1));
    }

    @Nested
    @Import({
        ApprovalTaskJobResolver.class, ApprovalTaskMethodSecurityTestConfiguration.class,
        ApprovalTaskFacadeIntTest.MethodSecurityFacadeConfiguration.class
    })
    class MethodSecurity {

        @Autowired
        private ApprovalTaskFacade securedApprovalTaskFacade;

        @Autowired
        private AuthorizationCheck authorizationCheck;

        @BeforeEach
        void setAuthentication() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();

            reset(authorizationCheck);
        }

        @Test
        void testNonAdminCannotManageApprovalTasks() {
            ApprovalTask approvalTask = approvalTaskService.create(manualApprovalTask("Review"));

            long approvalTaskId = Objects.requireNonNull(approvalTask.getId());

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.createManualApprovalTask(manualApprovalTask("Other")));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.deleteApprovalTask(approvalTaskId));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.getApprovalTask(approvalTaskId));
            assertThat(securedApprovalTaskFacade.getApprovalTasks((Integer) null)).isEmpty();
            assertThat(securedApprovalTaskFacade.getApprovalTasks(List.of(approvalTaskId))).isEmpty();
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.getPendingApprovals(null));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.updateApprovalTask(approvalTask));

            assertThat(approvalTaskService.getApprovalTasks()).singleElement()
                .extracting(ApprovalTask::getName)
                .isEqualTo("Review");
        }

        @Test
        void testAdminManagesManualApprovalTasks() {
            when(authorizationCheck.isTenantAdmin()).thenReturn(true);

            ApprovalTask manualApprovalTask = manualApprovalTask("Review");

            manualApprovalTask.setEnvironment(Environment.PRODUCTION);

            ApprovalTask approvalTask = securedApprovalTaskFacade.createManualApprovalTask(manualApprovalTask);

            assertThat(approvalTaskService.getApprovalTask(Objects.requireNonNull(approvalTask.getId())))
                .extracting(ApprovalTask::getJobResumeId)
                .isNull();
            assertThat(securedApprovalTaskFacade.getApprovalTasks(Environment.PRODUCTION.ordinal()))
                .extracting(ApprovalTask::getId)
                .containsExactly(approvalTask.getId());
            assertThat(securedApprovalTaskFacade.getApprovalTasks(Environment.DEVELOPMENT.ordinal())).isEmpty();

            when(authorizationCheck.hasPermission(approvalTask.getId(), "ApprovalTask", "DEPLOYMENT_EDIT"))
                .thenReturn(true);

            securedApprovalTaskFacade.deleteApprovalTask(Objects.requireNonNull(approvalTask.getId()));

            assertThat(approvalTaskService.countApprovalTasks()).isZero();
        }

        @Test
        void testOwnerReadsAndUpdatesTheirTaskWithoutAnAdmin() {
            ApprovalTask approvalTask = approvalTaskService.create(manualApprovalTask("Review"));

            long approvalTaskId = Objects.requireNonNull(approvalTask.getId());

            when(authorizationCheck.isResourceOwner(approvalTaskId, "ApprovalTask")).thenReturn(true);

            assertThat(securedApprovalTaskFacade.getApprovalTask(approvalTaskId))
                .extracting(ApprovalTask::getName)
                .isEqualTo("Review");
            assertThat(securedApprovalTaskFacade.getApprovalTasks(List.of(approvalTaskId)))
                .extracting(ApprovalTask::getId)
                .containsExactly(approvalTaskId);

            approvalTask.setName("Reviewed");

            assertThat(securedApprovalTaskFacade.updateApprovalTask(approvalTask))
                .extracting(ApprovalTask::getName)
                .isEqualTo("Reviewed");
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.deleteApprovalTask(approvalTaskId));
        }

        @Test
        void testRunCreatesItsApprovalTaskWithoutAnAdmin() {
            long jobId = saveJob(Job.Status.STOPPED, null);

            when(authorizationCheck.hasPermission(jobId, "Job", "DEPLOYMENT_EDIT")).thenReturn(true);

            ApprovalTask approvalTask = securedApprovalTaskFacade.createApprovalTask(approvalTask(jobResumeId(jobId)));

            assertThat(approvalTaskService.getApprovalTask(Objects.requireNonNull(approvalTask.getId())))
                .extracting(ApprovalTask::getEnvironment)
                .isEqualTo(Environment.DEVELOPMENT);
        }

        @Test
        void testRunWithoutDeploymentEditOnItsJobCannotCreateItsApprovalTask() {
            long jobId = saveJob(Job.Status.STOPPED, null);

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> securedApprovalTaskFacade.createApprovalTask(approvalTask(signedToken(jobId))));

            verify(authorizationCheck).hasPermission(jobId, "Job", "DEPLOYMENT_EDIT");

            assertThat(approvalTaskService.countApprovalTasks()).isZero();
        }

        @Test
        void testUnauthenticatedRunCannotCreateItsApprovalTask() {
            SecurityContextHolder.clearContext();

            long jobId = saveJob(Job.Status.STOPPED, null);

            assertThatThrownBy(() -> securedApprovalTaskFacade.createApprovalTask(approvalTask(jobResumeId(jobId))))
                .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);

            assertThat(approvalTaskService.countApprovalTasks()).isZero();
        }

        private static ApprovalTask manualApprovalTask(String name) {
            ApprovalTask approvalTask = new ApprovalTask();

            approvalTask.setName(name);

            return approvalTask;
        }
    }

    static class MethodSecurityFacadeConfiguration {

        @Bean
        ApprovalTaskFacade securedApprovalTaskFacade(
            ApprovalTaskService approvalTaskService, ApprovalTokens approvalTokens, JobService jobService,
            PrincipalJobService principalJobService, TaskExecutionService taskExecutionService) {

            return new ApprovalTaskFacadeImpl(
                approvalTaskService, approvalTokens, jobService, principalJobService,
                mock(ProjectDeploymentService.class), PUBLIC_URL, taskExecutionService, mock(WorkflowService.class));
        }
    }
}
