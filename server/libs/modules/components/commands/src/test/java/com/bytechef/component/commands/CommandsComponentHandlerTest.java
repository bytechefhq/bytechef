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

package com.bytechef.component.commands;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PROCESS;
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
import com.bytechef.platform.component.runner.external.ProcessTaskRunner;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CommandsComponentHandlerTest {

    private final ComponentDefinition componentDefinition = new CommandsComponentHandler(
        createTaskRunnerRegistry()).getDefinition();

    @Test
    void testGetDefinition() {
        JsonFileAssert.assertEquals("definition/commands_v1.json", componentDefinition);
    }

    @Test
    void testEveryActionOffersTheProcessTaskRunnerOnly() {
        List<ActionDefinition> actionDefinitions = componentDefinition.getActions().orElseThrow();

        assertThat(actionDefinitions).hasSize(3);

        for (ActionDefinition actionDefinition : actionDefinitions) {
            StringProperty typeProperty = findTaskRunnerTypeProperty(actionDefinition);

            List<String> optionValues = typeProperty.getOptions().orElseThrow()
                .stream()
                .map(Option::getValue)
                .toList();

            assertThat(optionValues).containsExactly(PROCESS);
            assertThat(typeProperty.getDefaultValue()).contains(PROCESS);
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
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Script script = applicationProperties.getScript();

        script.setRunners(Map.of(GRAALVM, enabledRunner(), PROCESS, enabledRunner()));

        return new TaskRunnerRegistryImpl(
            List.of(new GraalVmTaskRunner(null, applicationProperties), new ProcessTaskRunner(applicationProperties)),
            applicationProperties);
    }

    private static ApplicationProperties.Script.Runner enabledRunner() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);

        return runner;
    }
}
