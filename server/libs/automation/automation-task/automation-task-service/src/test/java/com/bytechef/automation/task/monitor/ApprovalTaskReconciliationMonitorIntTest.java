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

package com.bytechef.automation.task.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.task.config.AutomationTaskIntTestConfiguration;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.scheduler.lock.config.SchedulerLockConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationTaskIntTestConfiguration.class)
class ApprovalTaskReconciliationMonitorIntTest {

    @MockitoSpyBean
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @MockitoSpyBean
    private JobService jobService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ApprovalTaskReconciliationMonitor approvalTaskReconciliationMonitor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM approval_task");
        jdbcTemplate.update("DELETE FROM job");
        jdbcTemplate.update("DELETE FROM public.shedlock");

        approvalTaskReconciliationMonitor = new ApprovalTaskReconciliationMonitor(
            approvalTaskService, approvalTokens, jobService, tenantService);
    }

    @Test
    void testFailedRunMarksTaskExpired() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.FAILED));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.EXPIRED);
    }

    @Test
    void testCancelledRunMarksTaskExpired() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.CANCELLED));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.EXPIRED);
    }

    @Test
    void testPurgedRunMarksTaskExpired() {
        long approvalTaskId = createApprovalTask(Long.MAX_VALUE);

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.EXPIRED);
    }

    @Test
    void testCompletedRunMarksTaskCompleted() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.COMPLETED));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.COMPLETED);
    }

    @Test
    void testRunningJobLeavesTaskUntouched() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.STARTED));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.OPEN);
    }

    @Test
    void testRunSuspendedOnThisApprovalLeavesTaskUntouched() {
        long jobId = saveJob(Job.Status.STOPPED);

        JobResumeId jobResumeId = JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId);

        setJobResumeId(jobId, jobResumeId);

        long approvalTaskId = createApprovalTask(jobResumeId.toString());

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.OPEN);
    }

    @Test
    void testStoppedRunSuspendedElsewhereMarksTaskCompleted() {
        long jobId = saveJob(Job.Status.STOPPED);

        setJobResumeId(jobId, JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId));

        long approvalTaskId = createApprovalTask(jobId);

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.COMPLETED);
    }

    @Test
    void testStoppedRunWithoutResumeIdMarksTaskExpired() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.STOPPED));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.EXPIRED);
    }

    @Test
    void testUnparseableResumeIdLeavesTaskUntouched() {
        long approvalTaskId = createApprovalTask("not-a-resume-id");

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.OPEN);
    }

    @Test
    void testSignedResumeTokenIsUnwrappedBeforeParsing() {
        long jobId = saveJob(Job.Status.COMPLETED);

        JobResumeId jobResumeId = JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId);

        long approvalTaskId = createApprovalTask(
            approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofDays(1)));

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.COMPLETED);
    }

    @Test
    void testTransientFetchFailureLeavesTaskUntouched() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.COMPLETED));

        doThrow(new IllegalStateException("connection reset"))
            .when(jobService)
            .fetchJob(anyLong());

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.OPEN);
    }

    @Test
    void testFailedUpdateDoesNotSkipTheRemainingTasks() {
        long firstApprovalTaskId = createApprovalTask(saveJob(Job.Status.COMPLETED));
        long secondApprovalTaskId = createApprovalTask(saveJob(Job.Status.COMPLETED));

        doThrow(new OptimisticLockingFailureException("stale"))
            .doCallRealMethod()
            .when(approvalTaskService)
            .resolve(any());

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(List.of(getStatus(firstApprovalTaskId), getStatus(secondApprovalTaskId)))
            .containsExactlyInAnyOrder(ApprovalTask.Status.OPEN, ApprovalTask.Status.COMPLETED);
    }

    @Test
    void testTaskResolvedConcurrentlyIsNotOverwritten() {
        long approvalTaskId = createApprovalTask(saveJob(Job.Status.COMPLETED));

        doAnswer(invocation -> {
            Object unresolvedApprovalTasks = invocation.callRealMethod();

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            transactionTemplate.executeWithoutResult(transactionStatus -> jdbcTemplate.update(
                "UPDATE approval_task SET status = ?, version = version + 1 WHERE id = ?",
                ApprovalTask.Status.EXPIRED.ordinal(), approvalTaskId));

            return unresolvedApprovalTasks;
        }).when(approvalTaskService)
            .getUnresolvedApprovalTasks();

        approvalTaskReconciliationMonitor.reconcileApprovalTasks();

        assertThat(getStatus(approvalTaskId)).isEqualTo(ApprovalTask.Status.EXPIRED);
    }

    @Test
    void testReconciliationRunsOnOneInstanceWhenEveryInstanceFiresIt() throws Exception {
        AtomicInteger sweeps = new AtomicInteger();

        try (AnnotationConfigApplicationContext firstInstance = createInstance(sweeps);
            AnnotationConfigApplicationContext secondInstance = createInstance(sweeps)) {

            fireConcurrently(firstInstance, secondInstance);

            assertThat(sweeps).hasValue(1);

            ApprovalTaskReconciliationMonitor lateApprovalTaskReconciliationMonitor = secondInstance.getBean(
                ApprovalTaskReconciliationMonitor.class);

            lateApprovalTaskReconciliationMonitor.reconcileApprovalTasks();

            assertThat(sweeps).hasValue(1);
        }

        assertThat(jdbcTemplate.queryForList("SELECT name FROM public.shedlock", String.class))
            .containsExactly("approvalTaskReconciliationMonitor");
    }

    private AnnotationConfigApplicationContext createInstance(AtomicInteger sweeps) {
        TenantService countingTenantService = mock(TenantService.class);

        when(countingTenantService.getTenantIds()).thenAnswer(invocation -> {
            sweeps.incrementAndGet();

            return List.of();
        });

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(DataSource.class, () -> new DelegatingDataSource(dataSource));
        applicationContext.registerBean(
            ApprovalTaskReconciliationMonitor.class,
            () -> new ApprovalTaskReconciliationMonitor(
                mock(ApprovalTaskService.class), approvalTokens, mock(JobService.class), countingTenantService));
        applicationContext.register(SchedulerLockConfiguration.class);

        applicationContext.refresh();

        return applicationContext;
    }

    private static void fireConcurrently(
        AnnotationConfigApplicationContext firstInstance, AnnotationConfigApplicationContext secondInstance)
        throws Exception {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

        List<CompletableFuture<Void>> firings = List.of(
            fire(firstInstance.getBean(ApprovalTaskReconciliationMonitor.class), cyclicBarrier),
            fire(secondInstance.getBean(ApprovalTaskReconciliationMonitor.class), cyclicBarrier));

        CompletableFuture<Void> allFirings = CompletableFuture.allOf(firings.toArray(CompletableFuture[]::new));

        allFirings.get();
    }

    private static CompletableFuture<Void> fire(
        ApprovalTaskReconciliationMonitor approvalTaskReconciliationMonitor, CyclicBarrier cyclicBarrier) {

        return CompletableFuture.runAsync(() -> {
            try {
                cyclicBarrier.await();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

            approvalTaskReconciliationMonitor.reconcileApprovalTasks();
        });
    }

    private long createApprovalTask(long jobId) {
        JobResumeId jobResumeId = JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId);

        return createApprovalTask(jobResumeId.toString());
    }

    private long createApprovalTask(String jobResumeId) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(jobResumeId);
        approvalTask.setName("Approve the order");

        approvalTask = approvalTaskService.create(approvalTask);

        return Objects.requireNonNull(approvalTask.getId());
    }

    private ApprovalTask.Status getStatus(long approvalTaskId) {
        ApprovalTask approvalTask = approvalTaskService.getApprovalTask(approvalTaskId);

        return approvalTask.getStatus();
    }

    private long saveJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private void setJobResumeId(long jobId, JobResumeId jobResumeId) {
        Job job = jobService.getJob(jobId);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeId.toString()));

        jobRepository.save(job);
    }
}
