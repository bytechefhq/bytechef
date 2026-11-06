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

package com.bytechef.platform.scheduler.db.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Ivica Cardic
 */
class SchedulerQuartzAutoStartupContractTest {

    private static final List<String> SHIPPED_APPLICATION_YAML_PATHS = List.of(
        "server/apps/server-app/src/main/resources/config/application.yml",
        "server/ee/apps/config-server-app/src/main/resources/config/apps/scheduler-app.yml");

    static Stream<String> shippedApplicationYamlPaths() {
        return SHIPPED_APPLICATION_YAML_PATHS.stream();
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    @ParameterizedTest
    @MethodSource("shippedApplicationYamlPaths")
    void testShippedApplicationYamlDisablesQuartzAutoStartup(String repoRelativePath) throws IOException {
        File repositoryRoot = findRepositoryRoot();
        File applicationYamlFile = new File(repositoryRoot, repoRelativePath);

        Assertions.assertThat(applicationYamlFile)
            .as("Expected to find %s under repository root %s", repoRelativePath, repositoryRoot)
            .isFile();

        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            repoRelativePath, new FileSystemResource(applicationYamlFile));

        boolean autoStartupDisabled = propertySources.stream()
            .anyMatch(propertySource -> Boolean.FALSE.equals(propertySource.getProperty("spring.quartz.auto-startup")));

        Assertions.assertThat(autoStartupDisabled)
            .as(
                "%s must declare spring.quartz.auto-startup: false so Spring Boot's ungated Quartz "
                    + "autoconfiguration (autoStartup defaults to true) never starts Quartz alongside db-scheduler",
                repoRelativePath)
            .isTrue();
    }

    private static File findRepositoryRoot() throws IOException {
        File candidate = new File("").getCanonicalFile();

        for (int level = 0; level < 15; level++) {
            if (new File(candidate, "settings.gradle.kts").isFile()) {
                return candidate;
            }

            File parent = candidate.getParentFile();

            if (parent == null) {
                break;
            }

            candidate = parent;
        }

        throw new IllegalStateException(
            "Could not locate the repository root (a directory containing settings.gradle.kts) by walking up from "
                + new File("").getAbsolutePath()
                + "; this test must fail rather than silently pass when it cannot find the shipped configuration "
                + "files it is supposed to check");
    }
}
