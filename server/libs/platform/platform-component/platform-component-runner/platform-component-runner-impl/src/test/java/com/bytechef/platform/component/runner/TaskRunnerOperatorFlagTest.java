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

import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerOperatorFlagTest {

    private static final String FLAG = "some-enabled";

    @Test
    public void testAbsentRunnerEntryIsNotEnabled() {
        assertThat(TaskRunnerOperatorFlag.isEnabled(new ApplicationProperties(), PROCESS, FLAG)).isFalse();
    }

    @Test
    public void testNullRunnersMapIsNotEnabled() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        script.setRunners(null);

        assertThat(TaskRunnerOperatorFlag.isEnabled(applicationProperties, PROCESS, FLAG)).isFalse();
    }

    @Test
    public void testNullPropertiesMapIsNotEnabled() {
        assertThat(isEnabled(null)).isFalse();
    }

    @Test
    public void testAbsentPropertyIsNotEnabled() {
        assertThat(isEnabled(Map.of())).isFalse();
    }

    @Test
    public void testEmptyValueIsNotEnabled() {
        assertThat(isEnabled(Map.of(FLAG, ""))).isFalse();
    }

    @Test
    public void testMalformedValueIsNotEnabled() {
        assertThat(isEnabled(Map.of(FLAG, "yes"))).isFalse();
    }

    @Test
    public void testAnotherRunnersFlagIsNotRead() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(Map.of(FLAG, "true"));

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put(TaskRunnerConstants.GRAALVM, runner);

        script.setRunners(runners);

        assertThat(TaskRunnerOperatorFlag.isEnabled(applicationProperties, PROCESS, FLAG)).isFalse();
    }

    @Test
    public void testLiteralTrueIsEnabled() {
        assertThat(isEnabled(Map.of(FLAG, "true"))).isTrue();
    }

    @Test
    public void testUpperCaseTrueIsEnabled() {
        assertThat(isEnabled(Map.of(FLAG, "TRUE"))).isTrue();
    }

    @Test
    public void testMixedCaseTrueIsEnabled() {
        assertThat(isEnabled(Map.of(FLAG, "True"))).isTrue();
    }

    @Test
    public void testGetPropertyNameNamesTheFullConfigurationKey() {
        assertThat(TaskRunnerOperatorFlag.getPropertyName(PROCESS, FLAG))
            .isEqualTo("bytechef.script.runners.process.properties.some-enabled");
    }

    private static boolean isEnabled(@Nullable Map<String, String> properties) {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);
        runner.setProperties(properties);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put(PROCESS, runner);

        script.setRunners(runners);

        return TaskRunnerOperatorFlag.isEnabled(applicationProperties, PROCESS, FLAG);
    }
}
