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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

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

    @Test
    public void testRunnerPropertiesBind() {
        applicationContextRunner
            .withPropertyValues(
                "bytechef.script.runners.graalvm.enabled=true",
                "bytechef.script.runners.graalvm.properties.trusted-enabled=true",
                "bytechef.script.runners.docker.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();

                ApplicationProperties applicationProperties = context.getBean(ApplicationProperties.class);

                assertThat(applicationProperties.getScript()
                    .getRunners()).containsOnlyKeys("graalvm", "docker");
                assertThat(applicationProperties.getScript()
                    .getRunners()
                    .get("graalvm")
                    .isEnabled()).isTrue();
                assertThat(applicationProperties.getScript()
                    .getRunners()
                    .get("graalvm")
                    .getProperties()).containsEntry("trusted-enabled", "true");
            });
    }

    @Test
    public void testRunnerPropertiesBindFromEnvironmentVariables() {
        applicationContextRunner
            .withPropertyValues(
                "bytechef.script.runners.aws-batch.enabled=false",
                "bytechef.script.runners.aws-batch.properties.max-log-bytes=1MB",
                "bytechef.script.runners.aws-batch.properties.max-wait=PT1H",
                "bytechef.script.runners.kubernetes.enabled=true")
            .withInitializer(applicationContext -> applicationContext.getEnvironment()
                .getPropertySources()
                .addFirst(new SystemEnvironmentPropertySource(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    Map.of(
                        "BYTECHEF_SCRIPT_RUNNERS_AWSBATCH_ENABLED", "true",
                        "BYTECHEF_SCRIPT_RUNNERS_KUBERNETES_PROPERTIES_MAXWAIT", "PT3H",
                        "BYTECHEF_SCRIPT_RUNNERS_AWSBATCH_PROPERTIES_MAXWAIT", "PT2H",
                        "BYTECHEF_SCRIPT_RUNNERS_GRAALVM_PROPERTIES_TRUSTED_ENABLED", "true"))))
            .run(context -> {
                assertThat(context).hasNotFailed();

                ApplicationProperties applicationProperties = context.getBean(ApplicationProperties.class);
                ApplicationProperties.Script script = applicationProperties.getScript();

                ApplicationProperties.Script.Runner awsBatchRunner = script.findRunner("aws-batch");

                assertThat(awsBatchRunner).isNotNull();
                assertThat(awsBatchRunner.isEnabled()).isTrue();
                assertThat(awsBatchRunner.propertiesByCanonicalName())
                    .containsEntry("max-wait", "PT2H")
                    .containsEntry("max-log-bytes", "1MB");

                ApplicationProperties.Script.Runner kubernetesRunner = script.findRunner("kubernetes");

                assertThat(kubernetesRunner).isNotNull();
                assertThat(kubernetesRunner.isEnabled()).isTrue();
                assertThat(kubernetesRunner.propertiesByCanonicalName()).containsEntry("max-wait", "PT3H");

                ApplicationProperties.Script.Runner graalVmRunner = script.findRunner("graalvm");

                assertThat(graalVmRunner).isNotNull();
                assertThat(graalVmRunner.propertiesByCanonicalName()).containsEntry("trusted-enabled", "true");
            });
    }

    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfiguration {
    }
}
