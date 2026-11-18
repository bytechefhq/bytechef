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

import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerTrustedModeTest {

    @Test
    public void testAbsentRunnerEntryIsNotTrusted() {
        assertThat(TaskRunnerTrustedMode.isEnabled(new ApplicationProperties(), GRAALVM)).isFalse();
    }

    @Test
    public void testNullPropertiesMapIsNotTrusted() {
        assertThat(isEnabled(null)).isFalse();
    }

    @Test
    public void testAbsentPropertyIsNotTrusted() {
        assertThat(isEnabled(Map.of())).isFalse();
    }

    @Test
    public void testEmptyValueIsNotTrusted() {
        assertThat(isEnabled(Map.of("trusted-enabled", ""))).isFalse();
    }

    @Test
    public void testMalformedValueIsNotTrusted() {
        assertThat(isEnabled(Map.of("trusted-enabled", "yes"))).isFalse();
    }

    @Test
    public void testLiteralTrueIsTrusted() {
        assertThat(isEnabled(Map.of("trusted-enabled", "true"))).isTrue();
    }

    @Test
    public void testUpperCaseTrueIsTrusted() {
        assertThat(isEnabled(Map.of("trusted-enabled", "TRUE"))).isTrue();
    }

    @Test
    public void testGetPropertyNameNamesTheFullConfigurationKey() {
        assertThat(TaskRunnerTrustedMode.getPropertyName(GRAALVM))
            .isEqualTo("bytechef.script.runners.graalvm.properties.trusted-enabled");
    }

    private static boolean isEnabled(@Nullable Map<String, String> properties) {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(properties);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put(GRAALVM, runner);

        script.setRunners(runners);

        return TaskRunnerTrustedMode.isEnabled(applicationProperties, GRAALVM);
    }
}
