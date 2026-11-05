/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Builds the pod a Kubernetes task runs in: a file sidecar as init container that receives the inputs, the author's
 * image as {@code main}, and a file sidecar that hands the outputs back before the pod completes.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class KubernetesPodFactory {

    static final String DONE_MARKER = "/bytechef/work/.done";
    static final String INIT_CONTAINER = "bytechef-init";
    static final String MAIN_CONTAINER = "main";
    static final String OUTPUT_CONTAINER = "bytechef-output";
    static final String READY_MARKER = "/bytechef/work/.ready";
    static final String WORK_DIRECTORY = "/bytechef/work";

    private static final Duration DEADLINE_MARGIN = Duration.ofSeconds(60);
    private static final String POD_NAME_PREFIX = "bytechef-run-";
    private static final String PULL_SECRET_PREFIX = "bytechef-pull-";
    private static final String VOLUME_NAME = "work";

    private KubernetesPodFactory() {
    }

    static Pod pod(
        RemoteJobSpec spec, KubernetesRunnerSettings settings, String namespace, Duration completionBudget,
        long maxTransferBytes, @Nullable String pullSecretName) {

        Map<String, String> labels = new LinkedHashMap<>(settings.labels());

        labels.putAll(KubernetesJobLabels.labels(spec.key()));

        Map<String, String> annotations = new LinkedHashMap<>(settings.annotations());

        annotations.put(KubernetesJobLabels.ANNOTATION_KEY, spec.key()
            .value());

        if (!settings.killGracePeriod()
            .isZero()) {

            annotations.put(KubernetesJobLabels.ANNOTATION_KILL_GRACE_PERIOD, settings.killGracePeriod()
                .toString());
        }

        if (pullSecretName != null) {
            annotations.put(KubernetesJobLabels.ANNOTATION_PULL_SECRET, pullSecretName);
        }

        PodBuilder podBuilder = new PodBuilder();

        podBuilder.withNewMetadata()
            .withGenerateName(POD_NAME_PREFIX)
            .withNamespace(namespace)
            .withLabels(labels)
            .withAnnotations(annotations)
            .endMetadata();

        podBuilder.withNewSpec()
            .withRestartPolicy("Never")
            .withActiveDeadlineSeconds(completionBudget.plus(DEADLINE_MARGIN)
                .toSeconds())
            .withServiceAccountName(settings.serviceAccountName())
            .withAutomountServiceAccountToken(settings.serviceAccountName() == null ? Boolean.FALSE : null)
            .withNodeSelector(settings.nodeSelector()
                .isEmpty() ? null : settings.nodeSelector())
            .withTolerations(
                settings.tolerations()
                    .stream()
                    .map(toleration -> new TolerationBuilder()
                        .withKey(toleration.key()
                            .isEmpty() ? null : toleration.key())
                        .withOperator(toleration.operator())
                        .withValue(toleration.value())
                        .withEffect(toleration.effect())
                        .build())
                    .toList())
            .withImagePullSecrets(
                pullSecretName == null ? List.of() : List.of(new LocalObjectReference(pullSecretName)))
            .addNewVolume()
            .withName(VOLUME_NAME)
            .withNewEmptyDir()
            .withSizeLimit(new Quantity(String.valueOf(Math.multiplyExact(maxTransferBytes, 2L))))
            .endEmptyDir()
            .endVolume()
            .withInitContainers(
                fileSidecar(INIT_CONTAINER, settings.fileSidecarImage(), "until [ -f " + READY_MARKER +
                    " ]; do sleep 1; done"))
            .withContainers(
                main(spec, settings),
                fileSidecar(OUTPUT_CONTAINER, settings.fileSidecarImage(), "until [ -f " + DONE_MARKER +
                    " ]; do sleep 1; done"))
            .endSpec();

        return podBuilder.build();
    }

    static Secret
        pullSecret(String namespace, RemoteJobKey key, KubernetesRunnerSettings.RegistryCredentials credentials) {
        Map<String, Object> registryEntry = new LinkedHashMap<>();

        registryEntry.put("username", credentials.username());
        registryEntry.put("password", credentials.password());
        registryEntry.put("auth",
            EncodingUtils.base64EncodeToString(credentials.username() + ":" + credentials.password()));

        String dockerConfigJson = JsonUtils.write(Map.of("auths", Map.of(credentials.registry(), registryEntry)));

        SecretBuilder secretBuilder = new SecretBuilder();

        secretBuilder.withNewMetadata()
            .withGenerateName(PULL_SECRET_PREFIX)
            .withNamespace(namespace)
            .withLabels(KubernetesJobLabels.labels(key))
            .endMetadata();

        secretBuilder.withType("kubernetes.io/dockerconfigjson");
        secretBuilder.withData(Map.of(".dockerconfigjson", EncodingUtils.base64EncodeToString(dockerConfigJson)));

        return secretBuilder.build();
    }

    private static Container fileSidecar(String name, String image, String script) {
        return new ContainerBuilder()
            .withName(name)
            .withImage(image)
            .withImagePullPolicy("IfNotPresent")
            .withCommand("sh", "-c", script)
            .withVolumeMounts(workMount())
            .withResources(
                new ResourceRequirementsBuilder()
                    .withRequests(Map.of("cpu", new Quantity("10m"), "memory", new Quantity("16Mi")))
                    .withLimits(Map.of("cpu", new Quantity("200m"), "memory", new Quantity("64Mi")))
                    .build())
            .build();
    }

    private static Container main(RemoteJobSpec spec, KubernetesRunnerSettings settings) {
        List<String> command = new ArrayList<>(List.of("sh", "-c", "exec \"$@\" 2>" + spec.remoteStderrPath(), "sh"));

        command.addAll(spec.interpreter());
        command.add(spec.remoteSourcePath());

        List<EnvVar> environment = spec.environment()
            .entrySet()
            .stream()
            .map(entry -> new EnvVar(entry.getKey(), entry.getValue(), null))
            .toList();

        Map<String, Quantity> requests = new LinkedHashMap<>();
        Map<String, Quantity> limits = new LinkedHashMap<>();

        settings.resourceRequests()
            .forEach((resourceName, value) -> requests.put(resourceName, new Quantity(value)));
        settings.resourceLimits()
            .forEach((resourceName, value) -> limits.put(resourceName, new Quantity(value)));

        return new ContainerBuilder()
            .withName(MAIN_CONTAINER)
            .withImage(settings.image())
            .withImagePullPolicy(pullPolicy(spec.pullPolicy()))
            .withCommand(command)
            .withWorkingDir(WORK_DIRECTORY)
            .withEnv(environment)
            .withVolumeMounts(workMount())
            .withResources(new ResourceRequirementsBuilder()
                .withRequests(requests)
                .withLimits(limits)
                .build())
            .build();
    }

    private static String pullPolicy(RemotePullPolicy pullPolicy) {
        return switch (pullPolicy) {
            case ALWAYS -> "Always";
            case IF_NOT_PRESENT -> "IfNotPresent";
            case NEVER -> "Never";
        };
    }

    private static VolumeMount workMount() {
        return new VolumeMountBuilder()
            .withName(VOLUME_NAME)
            .withMountPath(WORK_DIRECTORY)
            .build();
    }
}
