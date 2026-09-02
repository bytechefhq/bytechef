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

package com.bytechef.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
public class ApplicationPropertiesScriptBindingTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    public void testSandboxPropertiesBind() {
        applicationContextRunner
            .withPropertyValues(
                "bytechef.script.sandbox.enabled=false",
                "bytechef.script.sandbox.max-cpu-time=30s",
                "bytechef.script.sandbox.max-heap-memory=64MB",
                "bytechef.script.sandbox.max-concurrent-executions=4")
            .run(context -> {
                assertThat(context).hasNotFailed();

                ApplicationProperties applicationProperties = context.getBean(ApplicationProperties.class);
                ApplicationProperties.Script.Sandbox sandbox = applicationProperties.getScript()
                    .getSandbox();

                assertThat(sandbox.isEnabled()).isFalse();
                assertThat(sandbox.getMaxCpuTime()).isEqualTo(Duration.ofSeconds(30));
                assertThat(sandbox.getMaxHeapMemory()
                    .toBytes()).isEqualTo(64L * 1024 * 1024);
                assertThat(sandbox.getMaxConcurrentExecutions()).isEqualTo(4);
            });
    }

    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfiguration {
    }
}
