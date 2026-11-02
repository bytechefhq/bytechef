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

package com.bytechef.platform.workflow.task.dispatcher;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.dynamicProperties;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TaskDispatcherDefinitionRegistryTest {

    @Disabled
    @Test
    public void testGetTaskDispatcherDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTaskDispatcherDefinitions() {
        // TODO
    }

    @Test
    public void testRegistryAcceptsDynamicPropertiesPropertyWithPropertiesFunction() {
        TaskDispatcherDefinition taskDispatcherDefinition = taskDispatcher("dispatcher")
            .properties(
                dynamicProperties("inputs")
                    .properties(inputParameters -> List.of()));

        TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry = new TaskDispatcherDefinitionRegistry(
            List.of(() -> taskDispatcherDefinition));

        assertThat(taskDispatcherDefinitionRegistry.getTaskDispatcherDefinitions())
            .containsExactly(taskDispatcherDefinition);
    }

    @Test
    public void testRegistryRejectsDynamicPropertiesPropertyWithoutPropertiesFunction() {
        TaskDispatcherDefinition taskDispatcherDefinition = taskDispatcher("dispatcher")
            .properties(
                dynamicProperties("inputs")
                    .propertiesLookupDependsOn("workflowUuid"));

        assertThatThrownBy(() -> new TaskDispatcherDefinitionRegistry(List.of(() -> taskDispatcherDefinition)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("inputs");
    }
}
