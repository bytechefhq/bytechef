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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerLimits;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class RemoteTaskRunnerSettingsTest {

    private final TaskRunnerLimits limits = TaskRunnerLimits.of(new ApplicationProperties(), "docker");

    @Test
    void testDefaultsMatchKestra() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(request(Map.of(), null), limits, "docker");

        assertThat(settings.resume()).isTrue();
        assertThat(settings.delete()).isTrue();
        assertThat(settings.waitUntilRunning()).isEqualTo(Duration.ofMinutes(10));
        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));
        assertThat(settings.pullPolicy()).isEqualTo(RemotePullPolicy.IF_NOT_PRESENT);
    }

    @Test
    void testTheTaskTimeoutTakesPrecedenceOverWaitUntilCompletion() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilCompletion", "PT30M"), Duration.ofSeconds(90)), limits, "docker");

        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void testWaitUntilCompletionIsClampedToTheOperatorsMaxWait() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilCompletion", "PT48H"), null), limits, "docker");

        assertThat(settings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));

        RemoteTaskRunnerSettings timeoutSettings = RemoteTaskRunnerSettings.of(
            request(Map.of(), Duration.ofHours(5)), limits, "docker");

        assertThat(timeoutSettings.waitUntilCompletion()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void testWaitUntilRunningNeverExceedsWaitUntilCompletion() {
        RemoteTaskRunnerSettings settings = RemoteTaskRunnerSettings.of(
            request(Map.of("waitUntilRunning", "PT20M"), Duration.ofMinutes(2)), limits, "docker");

        assertThat(settings.waitUntilRunning()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void testAMalformedDurationNamesTheProperty() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("waitUntilRunning", "ten minutes"), null), limits,
                "docker"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("waitUntilRunning")
                    .hasMessageContaining("ten minutes");
    }

    @Test
    void testANonPositiveDurationIsRejected() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("waitUntilCompletion", "PT0S"), null), limits, "docker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waitUntilCompletion");
    }

    @Test
    void testAnUnknownPullPolicyIsRejected() {
        assertThatThrownBy(
            () -> RemoteTaskRunnerSettings.of(request(Map.of("pullPolicy", "SOMETIMES"), null), limits, "docker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOMETIMES");
    }

    @Test
    void testPropertiesAreFreshOnEveryCall() {
        assertThat(RemoteTaskRunnerSettings.properties()
            .getFirst()).isNotSameAs(
                RemoteTaskRunnerSettings.properties()
                    .getFirst());

        assertThat(RemoteTaskRunnerSettings.properties())
            .extracting(property -> property.getName())
            .containsExactly("pullPolicy", "resume", "delete", "waitUntilRunning", "waitUntilCompletion");
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters, @Nullable Duration timeout) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), timeout,
            Map.of(), mock(ActionContext.class));
    }
}
