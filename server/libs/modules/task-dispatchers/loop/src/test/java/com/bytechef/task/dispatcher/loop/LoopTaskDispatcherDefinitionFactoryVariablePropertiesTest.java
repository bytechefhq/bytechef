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

package com.bytechef.task.dispatcher.loop;

import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableObjectProperty;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoopTaskDispatcherDefinitionFactory#variableProperties(Map)}.
 *
 * <p>
 * These tests verify that the loop task dispatcher always returns item and index variable properties, even when the
 * items list is empty or not present. This is important for the Data Pill popup to show loop variables when editing
 * nested tasks.
 *
 * @author Ivica Cardic
 */
class LoopTaskDispatcherDefinitionFactoryVariablePropertiesTest {

    @Test
    void testVariablePropertiesReturnsItemAndIndexWhenItemsNotPresent() {
        OutputResponse outputResponse = LoopTaskDispatcherDefinitionFactory.variableProperties(Map.of());

        assertNotNull(outputResponse, "OutputResponse should not be null when items is not present");
        assertOutputSchemaHasItemAndIndex(outputResponse);
    }

    @Test
    void testVariablePropertiesReturnsItemAndIndexWhenItemsIsEmpty() {
        OutputResponse outputResponse = LoopTaskDispatcherDefinitionFactory.variableProperties(
            Map.of(ITEMS, List.of()));

        assertNotNull(outputResponse, "OutputResponse should not be null when items list is empty");
        assertOutputSchemaHasItemAndIndex(outputResponse);
    }

    @Test
    void testVariablePropertiesReturnsItemAndIndexWhenItemsHasValues() {
        Map<String, Object> sampleItem = Map.of("name", "test", "value", 123);

        OutputResponse outputResponse = LoopTaskDispatcherDefinitionFactory.variableProperties(
            Map.of(ITEMS, List.of(sampleItem)));

        assertNotNull(outputResponse, "OutputResponse should not be null when items has values");
        assertOutputSchemaHasItemAndIndex(outputResponse);
        assertNotNull(outputResponse.getSampleOutput(), "Sample output should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> sampleOutput = (Map<String, Object>) outputResponse.getSampleOutput();

        assertTrue(sampleOutput.containsKey(ITEM), "Sample output should contain item key");
        assertTrue(sampleOutput.containsKey(INDEX), "Sample output should contain index key");
        assertEquals(0, sampleOutput.get(INDEX), "Index should be 0");
    }

    @Test
    void testVariablePropertiesSampleOutputHasDefaultValuesWhenItemsNotPresent() {
        OutputResponse outputResponse = LoopTaskDispatcherDefinitionFactory.variableProperties(Map.of());

        assertNotNull(outputResponse.getSampleOutput(), "Sample output should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> sampleOutput = (Map<String, Object>) outputResponse.getSampleOutput();

        assertTrue(sampleOutput.containsKey(ITEM), "Sample output should contain item key");
        assertTrue(sampleOutput.containsKey(INDEX), "Sample output should contain index key");
        assertEquals(Map.of(), sampleOutput.get(ITEM), "Default item should be empty map");
        assertEquals(0, sampleOutput.get(INDEX), "Default index should be 0");
    }

    private void assertOutputSchemaHasItemAndIndex(OutputResponse outputResponse) {
        BaseValueProperty<?> outputSchema = outputResponse.getOutputSchema();

        assertNotNull(outputSchema, "Output schema should not be null");
        assertTrue(outputSchema instanceof ModifiableObjectProperty, "Output schema should be an ObjectProperty");

        ModifiableObjectProperty objectProperty = (ModifiableObjectProperty) outputSchema;
        List<? extends BaseProperty> properties = objectProperty.getProperties()
            .orElse(List.of());

        assertEquals(2, properties.size(), "Should have 2 properties (item and index)");
        assertEquals(ITEM, properties.get(0)
            .getName());
        assertEquals(INDEX, properties.get(1)
            .getName());
    }
}
