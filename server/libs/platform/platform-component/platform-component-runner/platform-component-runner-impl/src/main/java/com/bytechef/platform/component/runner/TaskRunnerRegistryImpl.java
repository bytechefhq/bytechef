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

import com.bytechef.config.ApplicationProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class TaskRunnerRegistryImpl implements TaskRunnerRegistry {

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerRegistryImpl.class);

    private final Map<String, TaskRunner> taskRunnerMap;
    private final ApplicationProperties applicationProperties;

    public TaskRunnerRegistryImpl(List<TaskRunner> taskRunners, ApplicationProperties applicationProperties) {
        Map<String, TaskRunner> taskRunnersByType = new LinkedHashMap<>();

        List<TaskRunner> sortedTaskRunners = taskRunners.stream()
            .sorted(Comparator.comparing(TaskRunner::getType))
            .toList();

        for (TaskRunner taskRunner : sortedTaskRunners) {
            taskRunnersByType.put(taskRunner.getType(), taskRunner);
        }

        this.taskRunnerMap = taskRunnersByType;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public TaskRunner getTaskRunner(String type) {
        TaskRunner taskRunner = taskRunnerMap.get(type);

        if (taskRunner == null || !isEnabled(type)) {
            throw new TaskRunnerNotEnabledException(type);
        }

        return taskRunner;
    }

    @Override
    public List<TaskRunner> getTaskRunners(Set<TaskRunnerCapability> requiredCapabilities) {
        return taskRunnerMap.values()
            .stream()
            .filter(taskRunner -> isEnabled(taskRunner.getType()))
            .filter(taskRunner -> {
                Set<TaskRunnerCapability> capabilities = taskRunner.getCapabilities();

                return capabilities.containsAll(requiredCapabilities);
            })
            .toList();
    }

    @PostConstruct
    public void logEnabledRunners() {
        for (TaskRunner taskRunner : taskRunnerMap.values()) {
            String type = taskRunner.getType();

            if (!isEnabled(type)) {
                continue;
            }

            if (GRAALVM.equals(type) && TaskRunnerTrustedMode.isEnabled(applicationProperties, type)) {
                log.warn(
                    "Trusted GraalVM task runner is enabled; scripts selecting it run inside this JVM with full " +
                        "host access. Enable it only on a single-tenant deployment you control.");
            } else if (!GRAALVM.equals(type)) {
                log.warn("Task runner '{}' is enabled; workflows may execute code outside the script sandbox", type);
            }
        }
    }

    private boolean isEnabled(String type) {
        Map<String, ApplicationProperties.Script.Runner> runners = applicationProperties.getScript()
            .getRunners();

        ApplicationProperties.Script.Runner runner = runners.get(type);

        return runner != null && runner.isEnabled();
    }
}
