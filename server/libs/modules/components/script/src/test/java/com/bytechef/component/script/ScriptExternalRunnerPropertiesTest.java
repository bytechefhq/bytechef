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

package com.bytechef.component.script;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.runner.GraalVmTaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.runner.TaskRunnerRegistryImpl;
import com.bytechef.platform.component.runner.external.ProcessTaskRunner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ScriptExternalRunnerPropertiesTest {

    @Test
    void testExternalPropertiesAppearOnJavaScriptWhenAnExternalRunnerIsEnabled() {
        List<String> names = actionPropertyNames(registryWith(GRAALVM, PROCESS), "javascript");

        assertThat(names).contains("env", "inputFiles", "outputFiles", "timeout");
    }

    @Test
    void testExternalPropertiesAppearOnPythonWhenAnExternalRunnerIsEnabled() {
        List<String> names = actionPropertyNames(registryWith(GRAALVM, PROCESS), "python");

        assertThat(names).contains("env", "inputFiles", "outputFiles", "timeout");
    }

    @Test
    void testExternalPropertiesAreAbsentFromJavaScriptWhenOnlyGraalVmIsEnabled() {
        List<String> names = actionPropertyNames(registryWith(GRAALVM), "javascript");

        assertThat(names).doesNotContain("env", "inputFiles", "outputFiles", "timeout");
    }

    @Test
    void testExternalPropertiesAreAbsentFromPythonWhenOnlyGraalVmIsEnabled() {
        List<String> names = actionPropertyNames(registryWith(GRAALVM), "python");

        assertThat(names).doesNotContain("env", "inputFiles", "outputFiles", "timeout");
    }

    private static List<String> actionPropertyNames(TaskRunnerRegistry taskRunnerRegistry, String actionName) {
        ComponentDefinition componentDefinition = new ScriptComponentHandler(null, taskRunnerRegistry).getDefinition();

        List<ActionDefinition> actionDefinitions = componentDefinition.getActions()
            .orElseThrow();

        ActionDefinition actionDefinition = actionDefinitions.stream()
            .filter(candidate -> actionName.equals(candidate.getName()))
            .findFirst()
            .orElseThrow();

        List<? extends Property> properties = actionDefinition.getProperties()
            .orElseThrow();

        return properties.stream()
            .map(Property::getName)
            .toList();
    }

    private static TaskRunnerRegistry registryWith(String... enabledTypes) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        for (String enabledType : enabledTypes) {
            ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

            runner.setEnabled(true);

            runners.put(enabledType, runner);
        }

        script.setRunners(runners);

        return new TaskRunnerRegistryImpl(
            List.of(new GraalVmTaskRunner(null, applicationProperties), new ProcessTaskRunner(applicationProperties)),
            applicationProperties);
    }
}
