/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class KubernetesRunnerSettingsTest {

    @Test
    void testDefaultsFollowTheLanguage() {
        assertThat(settings("python", Map.of()).image()).isEqualTo("python:3.12-slim");
        assertThat(settings("javascript", Map.of()).image()).isEqualTo("node:22-alpine");
        assertThat(settings("js", Map.of()).image()).isEqualTo("node:22-alpine");
        assertThat(settings("shell", Map.of()).image()).isEqualTo("ubuntu:24.04");

        KubernetesRunnerSettings settings = settings("python", Map.of());

        assertThat(settings.fileSidecarImage()).isEqualTo("busybox:1.36");
        assertThat(settings.killGracePeriod()).isEqualTo(Duration.ZERO);
        assertThat(settings.namespace()).isNull();
        assertThat(settings.namespaceOr("default")).isEqualTo("default");
    }

    @Test
    void testEveryPropertyIsRead() {
        KubernetesRunnerSettings settings = settings(
            "python",
            Map.ofEntries(
                Map.entry("image", "registry.example.com/team/python:3.12"), Map.entry("namespace", "jobs"),
                Map.entry("serviceAccountName", "runner"),
                Map.entry(
                    "resources",
                    Map.of("cpuRequest", "250m", "cpuLimit", "1", "memoryRequest", "128Mi", "memoryLimit", "1Gi")),
                Map.entry("nodeSelector", Map.of("pool", "batch")),
                Map.entry(
                    "tolerations",
                    List.of(Map.of("key", "dedicated", "operator", "Equal", "value", "batch", "effect", "NoSchedule"))),
                Map.entry("labels", Map.of("team", "data")), Map.entry("annotations", Map.of("owner", "data-team")),
                Map.entry(
                    "credentials", Map.of("registry", "registry.example.com", "username", "u", "password", "p")),
                Map.entry("fileSidecarImage", "busybox:1.37"), Map.entry("killGracePeriod", "PT10S")));

        assertThat(settings.image()).isEqualTo("registry.example.com/team/python:3.12");
        assertThat(settings.namespaceOr("default")).isEqualTo("jobs");
        assertThat(settings.serviceAccountName()).isEqualTo("runner");
        assertThat(settings.resourceRequests()).containsEntry("cpu", "250m")
            .containsEntry("memory", "128Mi");
        assertThat(settings.resourceLimits()).containsEntry("cpu", "1")
            .containsEntry("memory", "1Gi");
        assertThat(settings.nodeSelector()).containsEntry("pool", "batch");
        assertThat(settings.tolerations()).containsExactly(
            new KubernetesRunnerSettings.Toleration("dedicated", "Equal", "batch", "NoSchedule"));
        assertThat(settings.labels()).containsEntry("team", "data");
        assertThat(settings.annotations()).containsEntry("owner", "data-team");
        assertThat(settings.credentials()).isEqualTo(
            new KubernetesRunnerSettings.RegistryCredentials("registry.example.com", "u", "p"));
        assertThat(settings.fileSidecarImage()).isEqualTo("busybox:1.37");
        assertThat(settings.killGracePeriod()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void testAMalformedQuantityNamesTheResource() {
        assertThatThrownBy(() -> settings("python", Map.of("resources", Map.of("memoryLimit", "lots"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimit")
            .hasMessageContaining("lots");
    }

    @Test
    void testAnOverlongQuantityIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("resources", Map.of("memoryLimit", "1".repeat(65)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimit");
    }

    @Test
    void testAPathologicalQuantityIsRejectedQuickly() {
        assertTimeoutPreemptively(
            Duration.ofSeconds(2),
            () -> assertThatThrownBy(
                () -> settings("python", Map.of("resources", Map.of("memoryLimit", "1".repeat(100_000) + "x"))))
                    .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void testWellFormedQuantitiesAreAccepted() {
        for (String quantity : List.of("500m", "1.5", "128Mi", "1e3", "2Gi")) {
            assertThatCode(() -> settings("python", Map.of("resources", Map.of("memoryLimit", quantity))))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testALabelOrAnnotationCannotUseTheReservedPrefix() {
        assertThatThrownBy(() -> settings("python", Map.of("labels", Map.of("bytechef.io/managed-by", "me"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.io/");

        assertThatThrownBy(() -> settings("python", Map.of("annotations", Map.of("bytechef.io/key", "x"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.io/");
    }

    @Test
    void testATolerationNeedsAKnownOperatorAndEffect() {
        assertThatThrownBy(
            () -> settings("python", Map.of("tolerations", List.of(Map.of("key", "k", "operator", "Near")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Near");

        assertThatThrownBy(
            () -> settings(
                "python",
                Map.of("tolerations", List.of(Map.of("key", "k", "operator", "Exists", "effect", "Sometimes")))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sometimes");
    }

    @Test
    void testCredentialsNeedAUsernameAndAPassword() {
        assertThatThrownBy(() -> settings("python", Map.of("credentials", Map.of("registry", "r", "username", "u"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("password");
    }

    @Test
    void testAMalformedOrNegativeKillGracePeriodIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("killGracePeriod", "soon")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("killGracePeriod");

        assertThatThrownBy(() -> settings("python", Map.of("killGracePeriod", "-PT1S")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("killGracePeriod");
    }

    @Test
    void testPropertiesAreFreshAndCoverEverySetting() {
        List<ModifiableValueProperty<?, ?>> properties = KubernetesRunnerSettings.properties();

        assertThat(properties.getFirst()).isNotSameAs(KubernetesRunnerSettings.properties()
            .getFirst());
        assertThat(properties).extracting(ValueProperty::getName)
            .containsExactly(
                "image", "entrypoint", "namespace", "serviceAccountName", "resources", "nodeSelector", "tolerations",
                "labels", "annotations", "credentials", "fileSidecarImage", "killGracePeriod");
    }

    private static KubernetesRunnerSettings settings(String languageId, Map<String, ?> parameters) {
        return KubernetesRunnerSettings.of(languageId, ParametersFactory.create(parameters));
    }
}
