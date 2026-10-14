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

import static com.bytechef.platform.component.runner.TaskRunnerConstants.CPU;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DOCKER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENV;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.EXTRA_HOSTS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.INPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MEMORY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.NETWORK_MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.OUTPUT_FILES;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PULL_POLICY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TIMEOUT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.definition.Property.StringProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.runner.GraalVmTaskRunner;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;
import com.bytechef.platform.component.runner.TaskRunnerRegistryImpl;
import com.bytechef.platform.component.runner.external.DockerTaskRunner;
import com.bytechef.platform.component.runner.external.ProcessTaskRunner;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The definition is configuration dependent: the {@code taskRunner} property is assembled from whichever runners the
 * registry reports as enabled. The registry is therefore built here explicitly - an incidentally empty registry would
 * let the snapshot record a feature that is not there without anything failing.
 *
 * <p>
 * All three built-in runners are enabled, which is deliberately <em>not</em> the shipped default. The snapshot this
 * test writes is what {@code generateDocumentation} turns into the published reference page's property table, so the
 * registry decides what the documentation describes: with GraalVM alone enabled the page named no external runner at
 * all, and {@code env}, {@code inputFiles}, {@code outputFiles}, {@code timeout} and every one of Docker's own settings
 * were absent from it while the component offered them. Enabling all three makes the page describe the full surface.
 *
 * <p>
 * GraalVM's trusted mode stays off, still matching the shipped default. It is an operator-gated escape hatch rather
 * than part of the surface, and the option is genuinely not offered until the flag is set - documenting it as if it
 * were always there would be the same mistake in the other direction.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptComponentHandlerTest {

    private static final List<String> ALWAYS_VISIBLE_PROPERTY_NAMES = List.of("input", "script", TASK_RUNNER);

    private final ComponentDefinition componentDefinition = new ScriptComponentHandler(
        null, createTaskRunnerRegistry()).getDefinition();

    @Test
    public void testGetDefinition() {
        JsonFileAssert.assertEquals("definition/script_v1.json", componentDefinition);
    }

    @Test
    public void testEveryActionOffersEveryEnabledTaskRunner() {
        List<ActionDefinition> actionDefinitions = componentDefinition.getActions()
            .orElseThrow();

        assertThat(actionDefinitions).isNotEmpty();

        for (ActionDefinition actionDefinition : actionDefinitions) {
            StringProperty typeProperty = findTaskRunnerTypeProperty(actionDefinition);

            List<String> optionValues = typeProperty.getOptions()
                .orElseThrow()
                .stream()
                .map(Option::getValue)
                .toList();

            assertThat(optionValues).containsExactly(DOCKER, GRAALVM, PROCESS);
            assertThat(typeProperty.getDefaultValue()).contains(GRAALVM);
        }
    }

    @Test
    public void testEveryActionOffersTheDockerRunnerProperties() {
        List<ActionDefinition> actionDefinitions = componentDefinition.getActions()
            .orElseThrow();

        for (ActionDefinition actionDefinition : actionDefinitions) {
            ObjectProperty taskRunnerProperty = findTaskRunnerProperty(actionDefinition);

            Map<String, Optional<String>> displayConditions = displayConditionsByName(
                taskRunnerProperty.getProperties()
                    .orElseThrow());

            assertThat(displayConditions).containsKeys(
                CPU, ENTRYPOINT, EXTRA_HOSTS, IMAGE, MEMORY, NETWORK_MODE, PULL_POLICY, USER);
        }
    }

    @Test
    public void testEveryRunnerContributedPropertyCarriesADisplayCondition() {
        List<ActionDefinition> actionDefinitions = componentDefinition.getActions()
            .orElseThrow();

        for (ActionDefinition actionDefinition : actionDefinitions) {
            Map<String, Optional<String>> displayConditions = displayConditionsByName(
                actionDefinition.getProperties()
                    .orElseThrow());

            ALWAYS_VISIBLE_PROPERTY_NAMES.forEach(displayConditions::remove);

            assertThat(displayConditions).containsKeys(ENV, INPUT_FILES, OUTPUT_FILES, TIMEOUT);

            assertDisplayConditionsArePresent(displayConditions, actionDefinition.getName());

            ObjectProperty taskRunnerProperty = findTaskRunnerProperty(actionDefinition);

            Map<String, Optional<String>> runnerDisplayConditions = displayConditionsByName(
                taskRunnerProperty.getProperties()
                    .orElseThrow());

            runnerDisplayConditions.remove(TYPE);

            assertDisplayConditionsArePresent(runnerDisplayConditions, actionDefinition.getName());
        }
    }

    private static void assertDisplayConditionsArePresent(
        Map<String, Optional<String>> displayConditions, String actionName) {

        for (Map.Entry<String, Optional<String>> entry : displayConditions.entrySet()) {
            assertThat(entry.getValue())
                .as("property '%s' of action '%s' must be shown only for the runners that support it",
                    entry.getKey(), actionName)
                .isPresent();
        }
    }

    private static Map<String, Optional<String>> displayConditionsByName(List<? extends Property> properties) {
        Map<String, Optional<String>> displayConditions = new LinkedHashMap<>();

        for (Property property : properties) {
            displayConditions.put(property.getName(), property.getDisplayCondition());
        }

        return displayConditions;
    }

    private static StringProperty findTaskRunnerTypeProperty(ActionDefinition actionDefinition) {
        ObjectProperty taskRunnerProperty = findTaskRunnerProperty(actionDefinition);

        return taskRunnerProperty.getProperties()
            .orElseThrow()
            .stream()
            .filter(property -> TYPE.equals(property.getName()))
            .map(StringProperty.class::cast)
            .findFirst()
            .orElseThrow();
    }

    private static ObjectProperty findTaskRunnerProperty(ActionDefinition actionDefinition) {
        List<? extends Property> properties = actionDefinition.getProperties()
            .orElseThrow();

        return properties.stream()
            .filter(property -> TASK_RUNNER.equals(property.getName()))
            .map(ObjectProperty.class::cast)
            .findFirst()
            .orElseThrow();
    }

    private static TaskRunnerRegistry createTaskRunnerRegistry() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        for (String type : List.of(DOCKER, GRAALVM, PROCESS)) {
            ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

            runner.setEnabled(true);

            runners.put(type, runner);
        }

        script.setRunners(runners);

        return new TaskRunnerRegistryImpl(
            List.of(
                new DockerTaskRunner(applicationProperties), new GraalVmTaskRunner(null, applicationProperties),
                new ProcessTaskRunner(applicationProperties)),
            applicationProperties);
    }
}
