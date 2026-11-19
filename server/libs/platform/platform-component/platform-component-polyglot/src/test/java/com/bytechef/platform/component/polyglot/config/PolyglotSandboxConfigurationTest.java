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

package com.bytechef.platform.component.polyglot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.polyglot.PolyglotSandboxSettings;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * @author Ivica Cardic
 */
public class PolyglotSandboxConfigurationTest {

    @Test
    public void testSandboxPropertiesMapToTheSandboxSettings() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script.Sandbox sandbox = applicationProperties.getScript()
            .getSandbox();

        sandbox.setEnabled(true);
        sandbox.setMaxConcurrentExecutions(16);
        sandbox.setMaxCpuTime(Duration.ofMinutes(5));
        sandbox.setMaxHeapMemory(DataSize.ofMegabytes(512));

        PolyglotSandboxConfiguration polyglotSandboxConfiguration = new PolyglotSandboxConfiguration(
            applicationProperties);

        assertThat(polyglotSandboxConfiguration.getPolyglotSandboxSettings())
            .isEqualTo(new PolyglotSandboxSettings(true, Duration.ofMinutes(5), 512L * 1024 * 1024, 16));
    }
}
