/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class KubernetesPodFactoryTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);

    @Test
    void testThePodHasTheInitMainAndOutputContainersSharingTheWorkVolume() {
        Pod pod = pod(Map.of(), null);

        assertThat(pod.getSpec()
            .getRestartPolicy()).isEqualTo("Never");
        assertThat(pod.getSpec()
            .getInitContainers()).extracting(Container::getName)
                .containsExactly("bytechef-init");
        assertThat(pod.getSpec()
            .getContainers()).extracting(Container::getName)
                .containsExactly("main", "bytechef-output");

        assertThat(pod.getSpec()
            .getVolumes()
            .getFirst()
            .getEmptyDir()
            .getSizeLimit()).isEqualTo(new Quantity(String.valueOf(2L * 1024 * 1024)));

        for (Container container : allContainers(pod)) {
            assertThat(container.getVolumeMounts()
                .getFirst()
                .getMountPath()).isEqualTo("/bytechef/work");
        }
    }

    @Test
    void testTheInitAndOutputContainersWaitForTheirMarkers() {
        Pod pod = pod(Map.of(), null);

        Container init = pod.getSpec()
            .getInitContainers()
            .getFirst();
        Container output = pod.getSpec()
            .getContainers()
            .get(1);

        assertThat(init.getImage()).isEqualTo("busybox:1.36");
        assertThat(String.join(" ", init.getCommand())).contains("/bytechef/work/.ready");
        assertThat(String.join(" ", output.getCommand())).contains("/bytechef/work/.done");
    }

    @Test
    void testMainRunsTheInterpreterOnTheSourceWithStderrRedirected() {
        Container main = pod(Map.of(), null).getSpec()
            .getContainers()
            .getFirst();

        assertThat(main.getImage()).isEqualTo("python:3.12-slim");
        assertThat(main.getImagePullPolicy()).isEqualTo("IfNotPresent");
        assertThat(main.getWorkingDir()).isEqualTo("/bytechef/work");
        assertThat(main.getCommand()).containsExactly(
            "sh", "-c", "exec \"$@\" 2>/bytechef/work/.stderr", "sh", "python3", "/bytechef/work/script.py");
        assertThat(main.getEnv()).anySatisfy(envVar -> {
            assertThat(envVar.getName()).isEqualTo("BYTECHEF_INPUT_FILE");
            assertThat(envVar.getValue()).isEqualTo("/bytechef/work/input.json");
        });
    }

    @Test
    void testLabelsAndAnnotationsCarryTheKeyAndTheAuthorsOwn() {
        Pod pod = pod(Map.of("labels", Map.of("team", "data"), "annotations", Map.of("owner", "x"),
            "killGracePeriod", "PT10S"), "bytechef-pull-abc");

        assertThat(pod.getMetadata()
            .getGenerateName()).isEqualTo("bytechef-run-");
        assertThat(pod.getMetadata()
            .getNamespace()).isEqualTo("jobs");
        assertThat(pod.getMetadata()
            .getLabels()).containsEntry("team", "data")
                .containsEntry("bytechef.io/managed-by", "bytechef")
                .containsEntry("bytechef.io/task-execution-id", "public-100-7")
                .containsEntry("bytechef.io/job-id", "100");
        assertThat(pod.getMetadata()
            .getAnnotations()).containsEntry("owner", "x")
                .containsEntry("bytechef.io/key", "public-100-7")
                .containsEntry("bytechef.io/kill-grace-period", "PT10S")
                .containsEntry("bytechef.io/pull-secret", "bytechef-pull-abc");
        assertThat(pod.getSpec()
            .getImagePullSecrets()
            .getFirst()
            .getName()).isEqualTo("bytechef-pull-abc");
    }

    @Test
    void testNoGracePeriodOrPullSecretMeansNoAnnotation() {
        Pod pod = pod(Map.of(), null);

        assertThat(pod.getMetadata()
            .getAnnotations()).doesNotContainKeys("bytechef.io/kill-grace-period", "bytechef.io/pull-secret");
        assertThat(pod.getSpec()
            .getImagePullSecrets()).isNullOrEmpty();
    }

    @Test
    void testTheDeadlineIsTheCompletionBudgetPlusAMinute() {
        assertThat(pod(Map.of(), null).getSpec()
            .getActiveDeadlineSeconds()).isEqualTo(3600L + 60);
    }

    @Test
    void testWithoutAServiceAccountTheTokenIsNotMounted() {
        assertThat(pod(Map.of(), null).getSpec()
            .getAutomountServiceAccountToken()).isFalse();

        Pod pod = pod(Map.of("serviceAccountName", "runner"), null);

        assertThat(pod.getSpec()
            .getServiceAccountName()).isEqualTo("runner");
        assertThat(pod.getSpec()
            .getAutomountServiceAccountToken()).isNull();
    }

    @Test
    void testResourcesSchedulingAndPullPolicyReachThePod() {
        Pod pod = KubernetesPodFactory.pod(
            spec(RemotePullPolicy.ALWAYS),
            KubernetesRunnerSettings.of(
                "python",
                ParametersFactory.create(
                    Map.of(
                        "resources", Map.of("cpuLimit", "2", "memoryRequest", "256Mi"),
                        "nodeSelector", Map.of("pool", "batch"),
                        "tolerations", List.of(Map.of("key", "dedicated", "operator", "Exists"))))),
            "jobs", Duration.ofHours(1), 1024L * 1024, null);

        Container main = pod.getSpec()
            .getContainers()
            .getFirst();

        assertThat(main.getImagePullPolicy()).isEqualTo("Always");
        assertThat(main.getResources()
            .getLimits()).containsEntry("cpu", new Quantity("2"));
        assertThat(main.getResources()
            .getRequests()).containsEntry("memory", new Quantity("256Mi"));
        assertThat(pod.getSpec()
            .getNodeSelector()).containsEntry("pool", "batch");
        assertThat(pod.getSpec()
            .getTolerations()
            .getFirst()
            .getOperator()).isEqualTo("Exists");
    }

    @Test
    void testThePullSecretIsADockerConfigJsonForTheRegistry() {
        Secret secret = KubernetesPodFactory.pullSecret(
            "jobs", KEY, new KubernetesRunnerSettings.RegistryCredentials("registry.example.com", "u", "p"));

        assertThat(secret.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
        assertThat(secret.getMetadata()
            .getGenerateName()).isEqualTo("bytechef-pull-");
        assertThat(secret.getMetadata()
            .getLabels()).containsEntry("bytechef.io/managed-by", "bytechef");

        String json = new String(
            EncodingUtils.base64Decode(secret.getData()
                .get(".dockerconfigjson")),
            StandardCharsets.UTF_8);

        assertThat(json).contains("registry.example.com")
            .contains(EncodingUtils.base64EncodeToString("u:p"));
    }

    private static List<Container> allContainers(Pod pod) {
        return Stream.concat(
            pod.getSpec()
                .getInitContainers()
                .stream(),
            pod.getSpec()
                .getContainers()
                .stream())
            .toList();
    }

    private static Pod pod(Map<String, ?> runnerParameters, String pullSecretName) {
        return KubernetesPodFactory.pod(
            spec(RemotePullPolicy.IF_NOT_PRESENT),
            KubernetesRunnerSettings.of("python", ParametersFactory.create(runnerParameters)), "jobs",
            Duration.ofHours(1), 1024L * 1024, pullSecretName);
    }

    private static RemoteJobSpec spec(RemotePullPolicy pullPolicy) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"),
            Map.of("HOME", "/bytechef/work", "BYTECHEF_INPUT_FILE", "/bytechef/work/input.json"), "/bytechef/work",
            pullPolicy, ParametersFactory.create(Map.of()));
    }
}
