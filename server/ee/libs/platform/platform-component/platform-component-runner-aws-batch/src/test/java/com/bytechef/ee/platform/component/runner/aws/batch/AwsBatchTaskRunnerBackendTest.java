/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.JOB_DEFINITION_ARN;
import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.JOB_ID;
import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.job;
import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.withOutputContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.services.batch.model.BatchException;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchTaskRunnerBackendTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);
    private static final Map<String, String> KEY_TAGS = Map.of(
        "bytechef.io/key", "public-100-7", "bytechef.io/prefix", "bytechef/100/public-100-7-abcd1234/");

    private final FakeAwsBatchOperations operations = new FakeAwsBatchOperations();
    private final AwsBatchTaskRunnerBackend backend = new AwsBatchTaskRunnerBackend(
        operations.factory(), Duration.ofMillis(5));

    @Test
    void testIdentity() {
        assertThat(backend.getType()).isEqualTo("aws-batch");
        assertThat(backend.getTitle()).isEqualTo("AWS Batch");
        assertThat(backend.getRemoteWorkingDirectory()).isEqualTo("/bytechef/work");
        assertThat(backend.getConnection()).hasValueSatisfying(connection -> {
            assertThat(connection.componentName()).isEqualTo("aws");
            assertThat(connection.componentVersion()).isEqualTo(1);
        });
    }

    @Test
    void testValidateChecksTheConnectionTheBudgetAndTheSettings() {
        assertThatThrownBy(() -> backend.validate(request(base()), context(base(), Map.of("region", "eu-west-1"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("access key");

        Map<String, Object> roleConnection = new HashMap<>(connection());

        roleConnection.put("roleArn", "arn:aws:iam::123456789012:role/bytechef");

        assertThatThrownBy(
            () -> backend.validate(request(base()), context(base(), roleConnection, Duration.ofHours(12))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 hours");

        backend.validate(request(base()), context(base(), connection()));
    }

    @Test
    void testSubmitStagesTheInputsRegistersTheDefinitionAndSubmits() throws Exception {
        byte[] archive = "tar-bytes".getBytes(StandardCharsets.UTF_8);

        RemoteJobHandle handle = backend.submit(
            spec(base()), new ByteArrayInputStream(archive), Deadline.after(Duration.ofMinutes(1)),
            context(base(), connection()));

        assertThat(handle.id()).isEqualTo(JOB_ID);
        assertThat(handle.attributes()).containsEntry("bucket", "bytechef-staging")
            .containsEntry("jobDefinitionArn", JOB_DEFINITION_ARN)
            .containsKey("prefix");

        String prefix = handle.attributes()
            .get("prefix");

        assertThat(prefix).startsWith("bytechef/100/public-100-7-");
        assertThat(operations.objects).containsEntry("bytechef-staging/" + prefix + "input.tar", archive);
        assertThat(operations.presignValidities).containsOnly(Duration.ofMinutes(70));
        assertThat(operations.registered.getFirst()
            .tags()).containsEntry("bytechef.io/prefix", prefix);
        assertThat(operations.submitted.getFirst()
            .jobQueue()).isEqualTo("bytechef");
        assertThat(operations.sessionDurations).contains(Duration.ofMinutes(70));
    }

    @Test
    void testSubmitSpoolsTheInputsIntoAnOwnerOnlyTempFile() throws Exception {
        assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix"));

        backend.submit(
            spec(base()), new ByteArrayInputStream("tar-bytes".getBytes(StandardCharsets.UTF_8)),
            Deadline.after(Duration.ofMinutes(1)), context(base(), connection()));

        assertThat(operations.stagedFilePermissions).containsExactly("rw-------");
    }

    @Test
    void testSubmitCleansUpWhenTheSubmitFails() {
        operations.failSubmit = awsError("ClientException", 400, "denied for secret-key");

        assertThatThrownBy(
            () -> backend.submit(
                spec(base()), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitting the job")
                    .hasMessageContaining("ClientException")
                    .hasMessageContaining("HTTP 400")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-key"));

        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);
        assertThat(operations.deletedPrefixes).hasSize(1);
    }

    @Test
    void testSubmitCleansUpWhenStagingFailsWithoutRegisteringAnything() {
        operations.failPutObject = awsError("AccessDenied", 403, "nope");

        assertThatThrownBy(
            () -> backend.submit(
                spec(base()), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AccessDenied");

        assertThat(operations.registered).isEmpty();
        assertThat(operations.deregistered).isEmpty();
    }

    @Test
    void testSubmitDeletesThePrefixWhenRegisteringTheDefinitionFails() {
        operations.failRegister = awsError("ClientException", 400, "bad definition");

        assertThatThrownBy(
            () -> backend.submit(
                spec(base()), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ClientException");

        assertThat(operations.deregistered).isEmpty();
        assertThat(operations.deletedPrefixes).hasSize(1);
        assertThat(operations.submitted).isEmpty();
    }

    @Test
    void testASuccessfulJobTailsItsLogAndReturnsMainsExitCode() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.RUNNABLE, null, null, 1L, KEY_TAGS));
        operations.jobSnapshots.add(job(JobStatus.RUNNING, null, null, 1L, KEY_TAGS));
        operations.jobSnapshots.add(job(JobStatus.SUCCEEDED, 0, null, 1L, KEY_TAGS));
        operations.logPages.add(new AwsBatchOperations.LogPage(List.of("hello", "from batch"), "t1"));

        CollectingSink logSink = new CollectingSink();

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), logSink,
            context(base(), connection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isZero();
        assertThat(logSink.stdout.toString()).contains("hello\n")
            .contains("from batch\n");
    }

    @Test
    void testMainsExitCodeWinsOverTheJobStatus() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.SUCCEEDED, 3, null, 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
            new CollectingSink(),
            context(base(), connection()));

        assertThat(exit.exitCode()).isEqualTo(3);
    }

    @Test
    void testAFailedJobWhoseMainNeverRanReportsTheStatusReason() throws Exception {
        operations.jobSnapshots.add(
            job(JobStatus.FAILED, null, "CannotPullContainerError: pull access denied", 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
            new CollectingSink(),
            context(base(), connection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason()).contains("CannotPullContainerError");
    }

    @Test
    void testAFailedOutputUploadFailsTheTaskEvenThoughMainSucceeded() throws Exception {
        operations.jobSnapshots.add(
            withOutputContainer(
                job(JobStatus.FAILED, 0, "Essential container in task exited", 1L, KEY_TAGS), 22,
                "curl: (22) The requested URL returned error: 403"));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
            new CollectingSink(), context(base(), connection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason())
            .isEqualTo("the output upload failed: curl: (22) The requested URL returned error: 403");
    }

    @Test
    void testAnOutputContainerThatNeverExitedFallsBackToTheJobStatusReason() throws Exception {
        operations.jobSnapshots.add(
            withOutputContainer(
                job(JobStatus.FAILED, 0, "Essential container in task exited", 1L, KEY_TAGS), null, null));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
            new CollectingSink(), context(base(), connection()));

        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason()).isEqualTo("the output upload failed: Essential container in task exited");
    }

    @Test
    void testAFailedJobWhoseOutputUploadSucceededKeepsMainsExitCode() throws Exception {
        operations.jobSnapshots.add(
            withOutputContainer(job(JobStatus.FAILED, 0, null, 1L, KEY_TAGS), 0, null));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
            new CollectingSink(), context(base(), connection()));

        assertThat(exit.exitCode()).isZero();
    }

    @Test
    void testTheTerminalLogDrainReadsPastAnEmptyPageUntilTheTokenStopsMoving() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.SUCCEEDED, 0, null, 1L, KEY_TAGS));
        operations.logPages.add(new AwsBatchOperations.LogPage(List.of("first"), "t1"));
        operations.logPages.add(new AwsBatchOperations.LogPage(List.of(), "t2"));
        operations.logPages.add(new AwsBatchOperations.LogPage(List.of("last"), "t3"));

        CollectingSink logSink = new CollectingSink();

        backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), logSink,
            context(base(), connection()));

        assertThat(logSink.stdout.toString()).isEqualTo("first\nlast\n");
    }

    @Test
    void testAJobThatNeverLeavesRunnableFailsAfterWaitUntilRunning() {
        operations.jobSnapshots.add(job(JobStatus.RUNNABLE, null, "Waiting for capacity", 1L, KEY_TAGS));

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMillis(50)), Deadline.after(Duration.ofHours(1)),
                new CollectingSink(), context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("did not start")
                    .hasMessageContaining("no capacity")
                    .hasMessageContaining("Waiting for capacity");
    }

    @Test
    void testARunningJobTimesOutAtTheCompletionDeadline() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.RUNNING, null, null, 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMillis(50)),
            new CollectingSink(), context(base(), connection()));

        assertThat(exit.finished()).isFalse();
    }

    @Test
    void testAnAbortedCallIsAnInterrupt() {
        operations.failDescribe = AbortedException.builder()
            .message("Thread was interrupted")
            .build();

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
                new CollectingSink(), context(base(), connection())))
                    .isInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread()
            .isInterrupted()).isFalse();
    }

    @Test
    void testCollectStreamsTheOutputArchiveOrReturnsNull() throws Exception {
        operations.objects.put("bytechef-staging/bytechef/100/public-100-7-abcd1234/output.tar", "out".getBytes());

        try (InputStream inputStream = backend.collect(
            handle(), Deadline.after(Duration.ofMinutes(1)), context(base(), connection()))) {

            assertThat(inputStream).isNotNull();
            assertThat(inputStream.readAllBytes()).isEqualTo("out".getBytes());
        }

        assertThat(operations.closeCount).isPositive();

        operations.objects.clear();

        assertThat(backend.collect(handle(), Deadline.after(Duration.ofMinutes(1)), context(base(), connection())))
            .isNull();
    }

    @Test
    void testFindReturnsTheNewestRunningJobWithTheKey() {
        operations.listedJobs = List.of(summary("old", JobStatus.RUNNING, 1L), summary("new", JobStatus.RUNNING, 2L));
        operations.describedById.put("old", jobWithId("old", JobStatus.RUNNING, 1L, KEY_TAGS));
        operations.describedById.put("new", jobWithId("new", JobStatus.RUNNING, 2L, KEY_TAGS));

        Optional<RemoteJobHandle> handle = backend.find(KEY, context(base(), connection()));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains("new");
        assertThat(handle.orElseThrow()
            .attributes()).containsEntry("prefix", "bytechef/100/public-100-7-abcd1234/")
                .containsEntry("bucket", "bytechef-staging")
                .containsEntry("jobDefinitionArn", JOB_DEFINITION_ARN);
    }

    @Test
    void testFindIgnoresTerminalJobsAndJobsWithAnotherKey() {
        operations.listedJobs =
            List.of(summary("done", JobStatus.SUCCEEDED, 3L), summary("other", JobStatus.RUNNING, 2L));
        operations.describedById.put(
            "other", jobWithId("other", JobStatus.RUNNING, 2L, Map.of("bytechef.io/key", "other-tenant-100-7")));

        assertThat(backend.find(KEY, context(base(), connection()))).isEmpty();
        assertThat(operations.terminated).isEmpty();
    }

    @Test
    void testCleanupModes() {
        backend.cleanup(handle(), CleanupMode.DELETE, context(base(), connection()));

        assertThat(operations.terminated).isEmpty();
        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);
        assertThat(operations.deletedPrefixes).containsExactly("bytechef-staging/bytechef/100/public-100-7-abcd1234/");

        operations.deregistered.clear();

        backend.cleanup(handle(), CleanupMode.KILL_AND_DELETE, context(base(), connection()));

        assertThat(operations.terminated).containsExactly(JOB_ID);
        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);

        operations.terminated.clear();
        operations.deregistered.clear();

        backend.cleanup(handle(), CleanupMode.DETACH, context(base(), connection()));

        assertThat(operations.terminated).isEmpty();
        assertThat(operations.deregistered).isEmpty();
    }

    @Test
    void testCleanupNeverThrowsAndRunsEvenWhileInterrupted() {
        operations.failWhileInterrupted = true;

        Thread.currentThread()
            .interrupt();

        try {
            assertThatCode(() -> backend.cleanup(handle(), CleanupMode.KILL_AND_DELETE, context(base(), connection())))
                .doesNotThrowAnyException();

            assertThat(operations.terminated).containsExactly(JOB_ID);
            assertThat(Thread.currentThread()
                .isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        operations.failCleanupCalls = new IllegalStateException("boom");

        assertThatCode(() -> backend.cleanup(handle(), CleanupMode.DELETE, context(base(), connection())))
            .doesNotThrowAnyException();
    }

    private static BatchException awsError(String errorCode, int statusCode, String message) {
        return (BatchException) BatchException.builder()
            .message(message)
            .statusCode(statusCode)
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode(errorCode)
                .errorMessage(message)
                .build())
            .build();
    }

    private static Map<String, Object> base() {
        return Map.of(
            "jobQueue", "bytechef", "bucket", "bytechef-staging", "executionRoleArn",
            "arn:aws:iam::123456789012:role/exec");
    }

    private static Map<String, Object> connection() {
        return Map.of("accessKeyId", "AKIA", "secretAccessKey", "secret-key", "region", "eu-west-1");
    }

    private static RemoteJobContext context(Map<String, ?> runnerParameters, Map<String, ?> connectionParameters) {
        return context(runnerParameters, connectionParameters, Duration.ofHours(1));
    }

    private static RemoteJobContext context(
        Map<String, ?> runnerParameters, Map<String, ?> connectionParameters, Duration completionBudget) {

        return new RemoteJobContext(
            new ComponentConnection("aws", 1, 1L, new HashMap<>(connectionParameters), null), Map.of(), null,
            ParametersFactory.create(runnerParameters), 1024L * 1024, completionBudget);
    }

    private static RemoteJobHandle handle() {
        return new RemoteJobHandle(
            JOB_ID,
            Map.of(
                "bucket", "bytechef-staging", "prefix", "bytechef/100/public-100-7-abcd1234/", "jobDefinitionArn",
                JOB_DEFINITION_ARN));
    }

    private static JobDetail jobWithId(
        String jobId, JobStatus status, long createdAt, Map<String, String> tags) {

        return job(status, null, null, createdAt, tags).toBuilder()
            .jobId(jobId)
            .build();
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, Map.of(),
            mock(ActionContext.class));
    }

    private static RemoteJobSpec spec(Map<String, ?> runnerParameters) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"), Map.of("HOME", "/bytechef/work"), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(runnerParameters));
    }

    private static JobSummary summary(String jobId, JobStatus status, long createdAt) {
        return JobSummary.builder()
            .jobId(jobId)
            .jobName("bytechef-public-100-7")
            .status(status)
            .createdAt(createdAt)
            .build();
    }

    static final class CollectingSink implements RemoteLogSink {

        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();

        @Override
        public void appendStdout(byte[] buffer, int length) {
            stdout.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }

        @Override
        public void appendStderr(byte[] buffer, int length) {
            stderr.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }
    }
}
