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

package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

public class TaskRunnerLimitsTest {

    @Test
    public void testBuiltInDefaultsApplyWhenNothingIsConfigured() {
        TaskRunnerLimits limits = TaskRunnerLimits.of(new ApplicationProperties(), "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofHours(1));
        assertThat(limits.getMaxLogBytes()).isEqualTo(1024L * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(512L * 1024 * 1024);
    }

    @Test
    public void testGlobalLimitsAreRead() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.RunnerLimits runnerLimits = applicationProperties.getScript()
            .getRunnerLimits();

        runnerLimits.setMaxWait(Duration.ofMinutes(20));
        runnerLimits.setMaxLogBytes(DataSize.ofKilobytes(64));
        runnerLimits.setMaxTransferBytes(DataSize.ofMegabytes(10));

        TaskRunnerLimits limits = TaskRunnerLimits.of(applicationProperties, "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofMinutes(20));
        assertThat(limits.getMaxLogBytes()).isEqualTo(64L * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(10L * 1024 * 1024);
    }

    @Test
    public void testARunnersOwnPropertiesOverrideTheGlobalLimits() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .getRunnerLimits()
            .setMaxWait(Duration.ofMinutes(20));

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of("max-wait", "PT5M", "max-log-bytes", "2MB", "max-transfer-bytes", "1GB"));

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        TaskRunnerLimits limits = TaskRunnerLimits.of(applicationProperties, "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofMinutes(5));
        assertThat(limits.getMaxLogBytes()).isEqualTo(2L * 1024 * 1024);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(1024L * 1024 * 1024);

        assertThat(TaskRunnerLimits.of(applicationProperties, "process")
            .getMaxWait()).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    public void testAMalformedOverrideNamesItsKey() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of("max-wait", "soon"));

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-wait");
    }

    @Test
    public void testANonPositiveMaxWaitOverrideIsRejected() {
        ApplicationProperties applicationProperties = withDockerProperties(Map.of("max-wait", "PT0S"));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-wait");
    }

    @Test
    public void testAMaxLogBytesOverrideBelowTwoIsRejected() {
        ApplicationProperties applicationProperties = withDockerProperties(Map.of("max-log-bytes", "1B"));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-log-bytes");
    }

    @Test
    public void testANonPositiveMaxTransferBytesOverrideIsRejected() {
        ApplicationProperties applicationProperties = withDockerProperties(Map.of("max-transfer-bytes", "0B"));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runners.docker.properties.max-transfer-bytes");
    }

    @Test
    public void testANonPositiveGlobalMaxWaitIsRejected() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .getRunnerLimits()
            .setMaxWait(Duration.ofSeconds(-1));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runner-limits.max-wait");
    }

    @Test
    public void testAGlobalMaxLogBytesBelowTwoIsRejected() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .getRunnerLimits()
            .setMaxLogBytes(DataSize.ofBytes(1));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runner-limits.max-log-bytes");
    }

    @Test
    public void testANonPositiveGlobalMaxTransferBytesIsRejected() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .getRunnerLimits()
            .setMaxTransferBytes(DataSize.ofBytes(0));

        assertThatThrownBy(() -> TaskRunnerLimits.of(applicationProperties, "docker"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.script.runner-limits.max-transfer-bytes");
    }

    @Test
    public void testTheSmallestValidValuesAreAccepted() {
        ApplicationProperties applicationProperties = withDockerProperties(
            Map.of("max-wait", "PT0.001S", "max-log-bytes", "2B", "max-transfer-bytes", "1B"));

        TaskRunnerLimits limits = TaskRunnerLimits.of(applicationProperties, "docker");

        assertThat(limits.getMaxWait()).isEqualTo(Duration.ofMillis(1));
        assertThat(limits.getMaxLogBytes()).isEqualTo(2L);
        assertThat(limits.getMaxTransferBytes()).isEqualTo(1L);
    }

    private static ApplicationProperties withDockerProperties(Map<String, String> properties) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(properties);

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("docker", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        return applicationProperties;
    }
}
