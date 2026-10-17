/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.POD_NAME;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.POD_UID;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.SECRET_NAME;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.pod;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.running;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.terminated;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.waiting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.SecretOwner;
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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class KubernetesTaskRunnerBackendTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);
    private static final Map<String, String> KEY_ANNOTATION = Map.of("bytechef.io/key", "public-100-7");

    private final FakeKubernetesOperations operations = new FakeKubernetesOperations();
    private final KubernetesTaskRunnerBackend backend = new KubernetesTaskRunnerBackend(
        operations.factory(), Duration.ofMillis(5));

    @AfterEach
    void clearTheInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void testIdentity() {
        assertThat(backend.getType()).isEqualTo("kubernetes");
        assertThat(backend.getTitle()).isEqualTo("Kubernetes");
        assertThat(backend.getRemoteWorkingDirectory()).isEqualTo("/bytechef/work");
        assertThat(backend.getConnection()).hasValueSatisfying(connection -> {
            assertThat(connection.componentName()).isEqualTo("kubernetes");
            assertThat(connection.componentVersion()).isEqualTo(1);
        });
    }

    @Test
    void testValidateChecksTheConnectionAndTheSettings() {
        assertThatThrownBy(() -> backend.validate(request(Map.of()), context(Map.of(), Map.of("masterUrl", "x"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("https://");

        assertThatThrownBy(
            () -> backend.validate(request(Map.of("killGracePeriod", "soon")), context(Map.of(), validConnection())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("killGracePeriod");

        backend.validate(request(Map.of()), context(Map.of(), validConnection()));
    }

    @Test
    void testSubmitCreatesThePodDeliversTheInputsAndMarksItReady() throws Exception {
        operations.podSnapshots.add(pod(waiting("PodInitializing", ""), null, null, KEY_ANNOTATION));
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));

        byte[] archive = "tar-bytes".getBytes(StandardCharsets.UTF_8);

        Map<String, ?> runnerParameters = Map.of("namespace", "jobs", "killGracePeriod", "PT10S");

        RemoteJobHandle handle = backend.submit(
            spec(runnerParameters), new ByteArrayInputStream(archive), Deadline.after(Duration.ofMinutes(1)),
            context(runnerParameters, validConnection()));

        assertThat(handle.id()).isEqualTo(POD_NAME);
        assertThat(handle.attributes()).containsEntry("namespace", "jobs")
            .containsEntry("killGracePeriod", "PT10S");

        Pod created = operations.createdPods.getFirst();

        assertThat(created.getMetadata()
            .getNamespace()).isEqualTo("jobs");
        assertThat(created.getSpec()
            .getActiveDeadlineSeconds()).isEqualTo(3600L + 60);

        assertThat(operations.execCalls).hasSize(2);
        assertThat(operations.execCalls.get(0)
            .container()).isEqualTo("bytechef-init");
        assertThat(operations.execCalls.get(0)
            .command()).containsExactly("sh", "-c", "head -c 9 | tar -xf - -C /bytechef/work");
        assertThat(operations.execCalls.get(0)
            .stdin()).isEqualTo(archive);
        assertThat(operations.execCalls.get(1)
            .command()).containsExactly("touch", "/bytechef/work/.ready");
        assertThat(operations.secretOwners).isEmpty();
    }

    @Test
    void testAFailureToTieThePullSecretToItsPodDoesNotFailTheSubmit() throws Exception {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));
        operations.failPatchSecretOwner = clientError("patch denied");

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        RemoteJobHandle handle = backend.submit(
            spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
            context(runnerParameters, validConnection()));

        assertThat(handle.attributes()).containsEntry("pullSecret", SECRET_NAME);
        assertThat(operations.deletedSecrets).isEmpty();
    }

    @Test
    void testSubmitCreatesAPullSecretFirstWhenCredentialsAreGiven() throws Exception {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r.example.com", "username", "u", "password", "p"));

        RemoteJobHandle handle = backend.submit(
            spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
            context(runnerParameters, validConnection()));

        assertThat(operations.createdSecrets).hasSize(1);
        assertThat(operations.secretOwners).containsExactly(new SecretOwner(SECRET_NAME, POD_NAME, POD_UID));
        assertThat(operations.createdPods.getFirst()
            .getMetadata()
            .getAnnotations()).containsEntry("bytechef.io/pull-secret", SECRET_NAME);
        assertThat(handle.attributes()).containsEntry("pullSecret", SECRET_NAME);
    }

    @Test
    void testSubmitSpoolsTheInputsIntoAnOwnerOnlyTempFile() throws Exception {
        assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix"));

        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));

        Map<String, ?> runnerParameters = Map.of();

        backend.submit(
            spec(runnerParameters), new ByteArrayInputStream("tar-bytes".getBytes(StandardCharsets.UTF_8)),
            Deadline.after(Duration.ofMinutes(1)), context(runnerParameters, validConnection()));

        assertThat(operations.deliveredFilePermissions).containsExactly("rw-------");
    }

    @Test
    void testSubmitCleansUpWhenDeliveringTheInputsFails() {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));
        operations.execResults.add(2);

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("delivering the inputs")
                    .hasMessageContaining("2");

        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testAnImagePullFailureFailsFast() {
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), waiting("ErrImagePull", "pull access denied for nope/nope"),
                running(), KEY_ANNOTATION));

        long startNanos = System.nanoTime();

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMinutes(10)), Deadline.after(Duration.ofHours(1)), sink(),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ErrImagePull")
                    .hasMessageContaining("pull access denied");

        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void testAPodThatNeverStartsFailsWithTheLatestWarningEvent() {
        operations.podSnapshots.add(pod(waiting("PodInitializing", ""), null, null, KEY_ANNOTATION));
        operations.warningEvent = "0/3 nodes are available: 3 Insufficient memory.";

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMillis(50)), Deadline.after(Duration.ofHours(1)), sink(),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("did not start")
                    .hasMessageContaining("Insufficient memory");
    }

    @Test
    void testASuccessfulJobFollowsItsLogsAndReturnsTheExitCode() throws Exception {
        operations.logText = "hello from the pod";
        operations.podSnapshots.add(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION));
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), terminated(0, "Completed"), running(), KEY_ANNOTATION));

        CollectingSink logSink = new CollectingSink();

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), logSink,
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isZero();
        assertThat(exit.reason()).isNull();
        assertThat(logSink.stdout.toString()).contains("hello from the pod");
    }

    @Test
    void testAnOomKilledContainerReportsItsReason() throws Exception {
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), terminated(137, "OOMKilled"), running(), KEY_ANNOTATION));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.exitCode()).isEqualTo(137);
        assertThat(exit.reason()).isEqualTo("OOMKilled");
    }

    @Test
    void testAnEvictedPodIsAFailureNamingTheEviction() throws Exception {
        Pod evicted = new PodBuilder(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION))
            .editStatus()
            .withPhase("Failed")
            .withReason("Evicted")
            .withMessage("The node was low on resource: memory.")
            .endStatus()
            .build();

        operations.podSnapshots.add(evicted);

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason()).contains("Evicted")
            .contains("low on resource");
    }

    @Test
    void testAwaitTimesOutWhenTheJobOutlivesTheCompletionDeadline() throws Exception {
        operations.podSnapshots.add(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMillis(50)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isFalse();
    }

    @Test
    void testCollectTarsTheOutputsFromTheSidecarAndMarksItDone() throws Exception {
        operations.collectBytes = "output-tar".getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = backend.collect(
            handle(), Deadline.after(Duration.ofMinutes(1)), context(Map.of(), validConnection()))) {

            assertThat(inputStream).isNotNull();
            assertThat(inputStream.readAllBytes()).isEqualTo(operations.collectBytes);
        }

        assertThat(operations.execCalls.get(0)
            .container()).isEqualTo("bytechef-output");
        assertThat(String.join(" ", operations.execCalls.get(0)
            .command())).contains("tar -cf - output");
        assertThat(operations.execCalls.get(1)
            .command()).containsExactly("touch", "/bytechef/work/.done");
    }

    @Test
    void testCollectFailsWhenTheSidecarCannotTarTheOutputs() {
        operations.execResults.add(1);

        assertThatThrownBy(
            () -> backend.collect(handle(), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("1");
    }

    @Test
    void testFindReturnsAPodPastTheInputStageWithItsSidecarRunning() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), running(), running(), annotations("PT10S", null)));

        Optional<RemoteJobHandle> handle = backend.find(KEY, context(Map.of(), validConnection()));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains(POD_NAME);
        assertThat(handle.orElseThrow()
            .attributes()).containsEntry("killGracePeriod", "PT10S");
        assertThat(operations.listedLabels).containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/task-execution-id", "public-100-7");
        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testFindDeletesAPodStillWaitingForItsInputs() {
        operations.listedPods = List.of(pod(running(), null, null, annotations(null, SECRET_NAME)));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testFindDeletesAPodWhoseOutputSidecarAlreadyExited() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), terminated(0, "Completed"), terminated(0, "Completed"), KEY_ANNOTATION));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).containsKey(POD_NAME);
    }

    @Test
    void testFindIgnoresAPodWhoseKeyAnnotationDiffers() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), running(), running(), Map.of("bytechef.io/key", "other-tenant-100-7")));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testFindUsesTheTasksNamespaceOverTheConnections() {
        backend.find(KEY, context(Map.of("namespace", "task-namespace"), validConnection()));

        assertThat(operations.listedNamespace).isEqualTo("task-namespace");

        backend.find(KEY, context(Map.of(), validConnection()));

        assertThat(operations.listedNamespace).isEqualTo("jobs");
    }

    @Test
    void testDeleteRemovesThePodAtOnceAndKillHonoursTheGracePeriod() {
        RemoteJobHandle handle = new RemoteJobHandle(
            POD_NAME, Map.of("namespace", "jobs", "killGracePeriod", "PT10S", "pullSecret", SECRET_NAME));

        backend.cleanup(handle, CleanupMode.DELETE, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).containsEntry(POD_NAME, Duration.ZERO);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);

        operations.deletedPods.clear();

        backend.cleanup(handle, CleanupMode.KILL_AND_DELETE, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).containsEntry(POD_NAME, Duration.ofSeconds(10));

        operations.deletedPods.clear();

        backend.cleanup(handle, CleanupMode.DETACH, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testCleanupNeverThrows() {
        operations.failDeletePod = true;

        RemoteJobHandle handle = new RemoteJobHandle(
            POD_NAME, Map.of("namespace", "jobs", "killGracePeriod", "garbage", "pullSecret", SECRET_NAME));

        assertThatCode(
            () -> backend.cleanup(handle, CleanupMode.KILL_AND_DELETE, context(Map.of(), validConnection())))
                .doesNotThrowAnyException();

        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testASecretCreationClientErrorIsWrappedAndLeaksNothing() {
        operations.failCreateSecret = clientError("secret creation denied for token secret-token");

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitting the job")
                    .hasMessageContaining("HTTP")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));

        assertThat(operations.createdPods).isEmpty();
    }

    @Test
    void testAnExecClientErrorIsWrappedAndThePodIsDeleted() {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));
        operations.failExec = clientError("exec denied for token secret-token");

        assertThatThrownBy(
            () -> backend.submit(
                spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitting the job")
                    .hasMessageContaining("HTTP")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));

        assertThat(operations.deletedPods).containsKey(POD_NAME);
    }

    @Test
    void testAClientErrorBuildingTheClientIsWrappedWhenSubmitting() {
        KubernetesTaskRunnerBackend failingBackend = new KubernetesTaskRunnerBackend(
            (connectionSettings, requestTimeout) -> {
                throw clientError("invalid client key for token secret-token");
            }, Duration.ofMillis(5));

        assertThatThrownBy(
            () -> failingBackend.submit(
                spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitting the job")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));
    }

    @Test
    void testThePullSecretIsDeletedWhenThePodCannotBeCreated() {
        operations.failCreatePod = clientError("pods is forbidden");

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                    .isInstanceOf(IllegalStateException.class);

        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testAClientErrorIsWrappedWithThePhaseAndNoSecrets() {
        Status status = new StatusBuilder().withCode(403)
            .withReason("Forbidden")
            .build();

        operations.failCreatePod =
            new KubernetesClientException("pods is forbidden for token secret-token", 403, status);

        assertThatThrownBy(
            () -> backend.submit(
                spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("creating the pod")
                    .hasMessageContaining("HTTP 403")
                    .hasMessageContaining("Forbidden")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));
    }

    @Test
    void testAnInterruptDuringAwaitIsAnInterruptNotAFailure() {
        operations.interruptGetPod = true;

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), sink(),
                context(Map.of(), validConnection())))
                    .isInstanceOf(InterruptedException.class)
                    .hasMessageContaining("waiting for the job")
                    .hasCauseInstanceOf(KubernetesClientException.class);

        assertThat(Thread.interrupted()).isFalse();
    }

    @Test
    void testAnInterruptWhileSubmittingDeletesThePodAndSecretAndIsAnInterrupt() {
        operations.interruptGetPod = true;

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                    .isInstanceOf(InterruptedException.class);

        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
        assertThat(Thread.interrupted()).isFalse();
    }

    @Test
    void testSubmitFailureCleanupDeletesDespiteAPendingInterruptAndKeepsIt() {
        operations.podSnapshots.add(pod(waiting("ErrImagePull", "pull access denied"), null, null, KEY_ANNOTATION));

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        Thread.currentThread()
            .interrupt();

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ErrImagePull");

        assertThat(Thread.interrupted()).isTrue();
        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testCleanupDeletesDespiteAPendingInterruptAndKeepsIt() {
        RemoteJobHandle handle = new RemoteJobHandle(
            POD_NAME, Map.of("namespace", "jobs", "killGracePeriod", "PT10S", "pullSecret", SECRET_NAME));

        Thread.currentThread()
            .interrupt();

        backend.cleanup(handle, CleanupMode.KILL_AND_DELETE, context(Map.of(), validConnection()));

        assertThat(Thread.interrupted()).isTrue();
        assertThat(operations.deletedPods).containsEntry(POD_NAME, Duration.ofSeconds(10));
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testAnExecClientErrorWrappedByTheAdapterIsWrappedWithItsPhase() {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));
        operations.failExec = new IOException(
            "The command failed in container bytechef-init", clientError("pods/exec is forbidden for secret-token"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("delivering the inputs")
                    .hasMessageContaining("HTTP 403 Forbidden")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));

        assertThat(operations.deletedPods).containsKey(POD_NAME);
    }

    @Test
    void testACollectExecClientErrorWrappedByTheAdapterIsWrappedWithItsPhase() {
        operations.failExec = new IOException(
            "The command failed in container bytechef-output", clientError("pods/exec is forbidden"));

        assertThatThrownBy(
            () -> backend.collect(handle(), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("collecting the outputs")
                    .hasMessageContaining("HTTP 403");
    }

    @Test
    void testAClientErrorWithoutAResponseSaysSoInsteadOfHttpZero() {
        for (KubernetesClientException clientException : List.of(
            new KubernetesClientException("Connection refused"),
            new KubernetesClientException("Connection refused", 0, null))) {

            operations.failCreatePod = clientException;

            assertThatThrownBy(
                () -> backend.submit(
                    spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                    context(Map.of(), validConnection())))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("creating the pod: no response from the API server")
                        .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("HTTP"));
        }
    }

    private static KubernetesClientException clientError(String message) {
        Status status = new StatusBuilder().withCode(403)
            .withReason("Forbidden")
            .build();

        return new KubernetesClientException(message, 403, status);
    }

    private static Map<String, String> annotations(String killGracePeriod, String pullSecret) {
        Map<String, String> annotations = new HashMap<>(KEY_ANNOTATION);

        if (killGracePeriod != null) {
            annotations.put("bytechef.io/kill-grace-period", killGracePeriod);
        }

        if (pullSecret != null) {
            annotations.put("bytechef.io/pull-secret", pullSecret);
        }

        return annotations;
    }

    private static RemoteJobContext context(Map<String, ?> runnerParameters, Map<String, ?> connectionParameters) {
        return new RemoteJobContext(
            new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(connectionParameters), null), Map.of(), null,
            ParametersFactory.create(runnerParameters), 1024L * 1024, Duration.ofHours(1));
    }

    private static RemoteJobHandle handle() {
        return new RemoteJobHandle(POD_NAME, Map.of("namespace", "jobs"));
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, Map.of(),
            mock(ActionContext.class));
    }

    private static RemoteLogSink sink() {
        return new CollectingSink();
    }

    private static RemoteJobSpec spec() {
        return spec(Map.of());
    }

    private static RemoteJobSpec spec(Map<String, ?> runnerParameters) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"), Map.of("HOME", "/bytechef/work"), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(runnerParameters));
    }

    private static Map<String, ?> validConnection() {
        return Map.of("masterUrl", "https://cluster:6443", "token", "secret-token", "namespace", "jobs");
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
