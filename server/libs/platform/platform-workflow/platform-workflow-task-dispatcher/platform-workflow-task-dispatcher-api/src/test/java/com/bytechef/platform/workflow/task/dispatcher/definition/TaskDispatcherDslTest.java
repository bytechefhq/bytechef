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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.dynamicProperties;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition.PropertiesFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableDynamicPropertiesProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableTaskDispatcherDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskDispatcherDslTest {

    @Test
    void testTaskDispatcherDefinitionPropertiesDefaultToEmptyLists() {
        ModifiableTaskDispatcherDefinition taskDispatcherDefinition = taskDispatcher("dispatcher");

        assertThat(taskDispatcherDefinition.getProperties()).isEmpty();
        assertThat(taskDispatcherDefinition.getTaskProperties()).isEmpty();
    }

    @Test
    void testTaskPropertiesIgnoresNullArray() {
        ModifiableTaskDispatcherDefinition taskDispatcherDefinition = taskDispatcher("dispatcher")
            .taskProperties((ModifiableProperty<?>[]) null);

        assertThat(taskDispatcherDefinition.getTaskProperties()).isEmpty();
    }

    @Test
    void testTaskPropertiesKeepsDeclaredProperties() {
        ModifiableTaskDispatcherDefinition taskDispatcherDefinition = taskDispatcher("dispatcher")
            .taskProperties(string("first"), string("second"));

        assertThat(taskDispatcherDefinition.getTaskProperties())
            .extracting(Property::getName)
            .containsExactly("first", "second");
    }

    @Test
    void testDynamicPropertiesDataSourceCarriesPropertiesFunction() {
        PropertiesFunction propertiesFunction = inputParameters -> List.of();

        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty = dynamicProperties("inputs", propertiesFunction)
            .propertiesLookupDependsOn("workflowUuid");

        PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        assertThat(propertiesDataSource.getPropertiesFunction()).isSameAs(propertiesFunction);
        assertThat(propertiesDataSource.getPropertiesLookupDependsOn()).containsExactly("workflowUuid");
    }

    @Test
    void testDynamicPropertiesDataSourceWithoutLookupDependenciesKeepsPropertiesFunction() {
        PropertiesFunction propertiesFunction = inputParameters -> List.of();

        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty = dynamicProperties("inputs", propertiesFunction);

        PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        assertThat(propertiesDataSource.getPropertiesFunction()).isSameAs(propertiesFunction);
        assertThat(propertiesDataSource.getPropertiesLookupDependsOn()).isEmpty();
    }

    @Test
    void testDynamicPropertiesRejectsNullPropertiesFunction() {
        assertThatThrownBy(() -> dynamicProperties("inputs", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("propertiesFunction");
    }
}
