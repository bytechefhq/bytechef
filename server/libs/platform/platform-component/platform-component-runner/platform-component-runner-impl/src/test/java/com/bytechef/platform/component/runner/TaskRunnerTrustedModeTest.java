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

import com.bytechef.config.ApplicationProperties;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerTrustedModeTest {

    @Test
    public void testUnsetTrustedIsNotTrusted() {
        assertThat(TaskRunnerTrustedMode.isEnabled(new ApplicationProperties())).isFalse();
    }

    @Test
    public void testDisabledTrustedIsNotTrusted() {
        assertThat(isEnabled(false)).isFalse();
    }

    @Test
    public void testEnabledTrustedIsTrusted() {
        assertThat(isEnabled(true)).isTrue();
    }

    @Test
    public void testGetPropertyNameNamesTheFullConfigurationKey() {
        assertThat(TaskRunnerTrustedMode.getPropertyName()).isEqualTo("bytechef.script.trusted.enabled");
    }

    private static boolean isEnabled(boolean enabled) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        ApplicationProperties.Script.Trusted trusted = script.getTrusted();

        trusted.setEnabled(enabled);

        return TaskRunnerTrustedMode.isEnabled(applicationProperties);
    }
}
