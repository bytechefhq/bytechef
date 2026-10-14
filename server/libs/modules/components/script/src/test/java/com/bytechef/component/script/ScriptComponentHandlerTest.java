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
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TYPE;
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
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The definition is configuration dependent: the {@code taskRunner} property is assembled from whichever runners the
 * registry reports as enabled. The registry is therefore built here explicitly, with GraalVM enabled and its trusted
 * mode left off, matching the shipped defaults - an incidentally empty registry would let the snapshot record a feature
 * that is not there without anything failing.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptComponentHandlerTest {

    private final ComponentDefinition componentDefinition = new ScriptComponentHandler(
        null, createTaskRunnerRegistry()).getDefinition();

    @Test
    public void testGetDefinition() {
        JsonFileAssert.assertEquals("definition/script_v1.json", componentDefinition);
    }

    @Test
    public void testEveryActionOffersTheGraalVmTaskRunner() {
        List<ActionDefinition> actionDefinitions = componentDefinition.getActions().orElseThrow();

        assertThat(actionDefinitions).isNotEmpty();

        for (ActionDefinition actionDefinition : actionDefinitions) {
            StringProperty typeProperty = findTaskRunnerTypeProperty(actionDefinition);

            List<String> optionValues = typeProperty.getOptions().orElseThrow()
                .stream()
                .map(Option::getValue)
                .toList();

            assertThat(optionValues).containsExactly(GRAALVM);
            assertThat(typeProperty.getDefaultValue()).contains(GRAALVM);
        }
    }

    private static StringProperty findTaskRunnerTypeProperty(ActionDefinition actionDefinition) {
        List<? extends Property> properties = actionDefinition.getProperties().orElseThrow();

        ObjectProperty taskRunnerProperty = properties.stream()
            .filter(property -> TASK_RUNNER.equals(property.getName()))
            .map(ObjectProperty.class::cast)
            .findFirst()
            .orElseThrow();

        return taskRunnerProperty.getProperties().orElseThrow()
            .stream()
            .filter(property -> TYPE.equals(property.getName()))
            .map(StringProperty.class::cast)
            .findFirst()
            .orElseThrow();
    }

    private static TaskRunnerRegistry createTaskRunnerRegistry() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        script.setRunners(Map.of(GRAALVM, runner));

        return new TaskRunnerRegistryImpl(
            List.of(new GraalVmTaskRunner(null, applicationProperties)), applicationProperties);
    }
}
