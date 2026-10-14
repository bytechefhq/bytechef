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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DockerOperatorSettingsTest {

    @Test
    void testAbsentRunnerEntryYieldsAClosedConfiguration() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(applicationProperties(null));

        assertThat(settings.getAllowedImages()).isEmpty();
        assertThat(settings.isHostNetworkAllowed()).isFalse();
        assertThat(settings.isVolumeMountAllowed()).isFalse();
        assertThat(settings.isImageAllowed("ubuntu:24.04")).isFalse();
    }

    @Test
    void testAnEmptyAllowlistPermitsNothing() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(applicationProperties(Map.of()));

        assertThat(settings.isImageAllowed("ubuntu:24.04")).isFalse();
    }

    @Test
    void testAllowedImagesAreParsedAsACommaSeparatedList() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-images", " ubuntu:24.04 , python:3.12-slim ")));

        assertThat(settings.getAllowedImages()).containsExactly("ubuntu:24.04", "python:3.12-slim");
        assertThat(settings.isImageAllowed("ubuntu:24.04")).isTrue();
        assertThat(settings.isImageAllowed("python:3.12-slim")).isTrue();
    }

    @Test
    void testAnImageOutsideTheAllowlistIsRejected() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-images", "ubuntu:24.04")));

        assertThat(settings.isImageAllowed("alpine:latest")).isFalse();
    }

    @Test
    void testAnUntaggedImageDoesNotMatchATaggedAllowlistEntry() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-images", "ubuntu:24.04")));

        assertThat(settings.isImageAllowed("ubuntu")).isFalse();
        assertThat(settings.isImageAllowed("ubuntu:latest")).isFalse();
    }

    @Test
    void testAPrefixOfAnAllowedImageIsRejected() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-images", "ubuntu:24.04")));

        assertThat(settings.isImageAllowed("ubuntu:24.04-evil")).isFalse();
        assertThat(settings.isImageAllowed("evil/ubuntu:24.04")).isFalse();
    }

    @Test
    void testBlankEntriesAreDropped() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-images", "ubuntu:24.04, ,,python:3.12-slim")));

        assertThat(settings.getAllowedImages()).containsExactly("ubuntu:24.04", "python:3.12-slim");
    }

    @Test
    void testAnAbsentNetworkModeAllowlistPermitsEveryModeTheRunnerKnows() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(applicationProperties(Map.of()));

        assertThat(settings.getAllowedNetworkModes()).containsExactlyElementsOf(
            DockerOperatorSettings.DEFAULT_ALLOWED_NETWORK_MODES);
        assertThat(settings.isNetworkModeAllowed("bridge")).isTrue();
        assertThat(settings.isNetworkModeAllowed("none")).isTrue();
        assertThat(settings.isNetworkModeAllowed("host")).isTrue();
    }

    @Test
    void testANamedNetworkModeAllowlistReplacesTheDefaultRatherThanAddingToIt() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-network-modes", " none ")));

        assertThat(settings.getAllowedNetworkModes()).containsExactly("none");
        assertThat(settings.isNetworkModeAllowed("none")).isTrue();
        assertThat(settings.isNetworkModeAllowed("bridge")).isFalse();
        assertThat(settings.isNetworkModeAllowed("host")).isFalse();
    }

    @Test
    void testANetworkModeAllowlistNamingNothingKnownPermitsNothing() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allowed-network-modes", "bytechef_default,container:bytechef")));

        assertThat(settings.isNetworkModeAllowed("bridge")).isFalse();
        assertThat(settings.isNetworkModeAllowed("none")).isFalse();
        assertThat(settings.isNetworkModeAllowed("host")).isFalse();
    }

    @Test
    void testTheResourceCeilingsFallBackToTheirBuiltInDefaults() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(applicationProperties(Map.of()));

        assertThat(settings.getDefaultCpu()).isEqualTo(DockerOperatorSettings.DEFAULT_CPU);
        assertThat(settings.getMaxCpu()).isEqualTo(DockerOperatorSettings.DEFAULT_MAX_CPU);
        assertThat(settings.getDefaultMemory()).isEqualTo(DockerOperatorSettings.DEFAULT_MEMORY);
        assertThat(settings.getMaxMemory()).isEqualTo(DockerOperatorSettings.DEFAULT_MAX_MEMORY);
        assertThat(settings.getPidsLimit()).isEqualTo(DockerOperatorSettings.DEFAULT_PIDS_LIMIT);
    }

    @Test
    void testTheResourceCeilingsAreTakenFromTheOperator() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(
                Map.of(
                    "default-cpu", " 0.5 ", "max-cpu", "8", "default-memory", " 128m ", "max-memory", "16g",
                    "pids-limit", " 64 ")));

        assertThat(settings.getDefaultCpu()).isEqualTo("0.5");
        assertThat(settings.getMaxCpu()).isEqualTo("8");
        assertThat(settings.getDefaultMemory()).isEqualTo("128m");
        assertThat(settings.getMaxMemory()).isEqualTo("16g");
        assertThat(settings.getPidsLimit()).isEqualTo("64");
    }

    @Test
    void testABlankCeilingFallsBackToItsBuiltInDefault() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("pids-limit", "   ", "max-memory", "")));

        assertThat(settings.getPidsLimit()).isEqualTo(DockerOperatorSettings.DEFAULT_PIDS_LIMIT);
        assertThat(settings.getMaxMemory()).isEqualTo(DockerOperatorSettings.DEFAULT_MAX_MEMORY);
    }

    @Test
    void testHostDefaultsToEmptyMeaningTheAmbientDaemon() {
        assertThat(DockerOperatorSettings.of(applicationProperties(Map.of()))
            .getHost()).isEmpty();
    }

    @Test
    void testMalformedBooleansFailClosed() {
        DockerOperatorSettings settings = DockerOperatorSettings.of(
            applicationProperties(Map.of("allow-host-network", "yes", "allow-volume-mounts", "")));

        assertThat(settings.isHostNetworkAllowed()).isFalse();
        assertThat(settings.isVolumeMountAllowed()).isFalse();
    }

    private static ApplicationProperties applicationProperties(Map<String, String> properties) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        if (properties == null) {
            script.setRunners(new HashMap<>());

            return applicationProperties;
        }

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(new HashMap<>(properties));

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        script.setRunners(runners);

        return applicationProperties;
    }
}
