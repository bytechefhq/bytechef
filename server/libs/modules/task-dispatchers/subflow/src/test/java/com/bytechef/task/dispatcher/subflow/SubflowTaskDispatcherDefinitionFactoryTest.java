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

package com.bytechef.task.dispatcher.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.definition.OutputDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertiesDataSource;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition.OutputFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition.PropertiesFunction;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class SubflowTaskDispatcherDefinitionFactoryTest {

    @Mock
    private SubflowDataSource subflowDataSource;

    private TaskDispatcherDefinition definition;

    @BeforeEach
    void setUp() {
        SubflowTaskDispatcherDefinitionFactory factory = new SubflowTaskDispatcherDefinitionFactory(subflowDataSource);
        definition = factory.getDefinition();
    }

    @Test
    void testGetTaskDispatcherDefinition() {
        SubflowDataSource stubDataSource = new SubflowDataSource() {

            @Override
            public OutputResponse getSubWorkflowInputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public OutputResponse getSubWorkflowOutputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String triggerName, String search) {
                return List.of();
            }
        };

        JsonFileAssert.assertEquals(
            "definition/subflow_v1.json",
            new SubflowTaskDispatcherDefinitionFactory(stubDataSource).getDefinition());
    }

    @Test
    void testInputsReturnsEmptyWhenWorkflowUuidIsNull() throws Exception {
        PropertiesFunction propertiesFunction = getDynamicPropertiesFunction();

        List<? extends Property> result = propertiesFunction.apply(Map.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void testInputsReturnsEmptyWhenWorkflowUuidIsEmpty() throws Exception {
        PropertiesFunction propertiesFunction = getDynamicPropertiesFunction();

        List<? extends Property> result = propertiesFunction.apply(Map.of("workflowUuid", ""));

        assertTrue(result.isEmpty());
    }

    @Test
    void testInputsReturnsEmptyWhenInputSchemaIsNull() throws Exception {
        when(subflowDataSource.getSubWorkflowInputSchema("test-uuid")).thenReturn(null);

        PropertiesFunction propertiesFunction = getDynamicPropertiesFunction();

        List<? extends Property> result = propertiesFunction.apply(Map.of("workflowUuid", "test-uuid"));

        assertTrue(result.isEmpty());
    }

    @Test
    void testOutputReturnsNullWhenWorkflowUuidIsNull() throws Exception {
        OutputFunction outputFunction = getOutputFunction();

        OutputResponse result = (OutputResponse) outputFunction.apply(Map.of());

        assertNull(result);
    }

    @Test
    void testOutputReturnsNullWhenWorkflowUuidIsEmpty() throws Exception {
        OutputFunction outputFunction = getOutputFunction();

        OutputResponse result = (OutputResponse) outputFunction.apply(Map.of("workflowUuid", ""));

        assertNull(result);
    }

    @Test
    void testOutputDelegatesToSubflowDataSource() throws Exception {
        OutputResponse expectedResponse = OutputResponse.of(
            com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string("test"));

        when(subflowDataSource.getSubWorkflowOutputSchema("test-uuid")).thenReturn(expectedResponse);

        OutputFunction outputFunction = getOutputFunction();

        OutputResponse result = (OutputResponse) outputFunction.apply(Map.of("workflowUuid", "test-uuid"));

        assertEquals(expectedResponse, result);
    }

    private PropertiesFunction getDynamicPropertiesFunction() {
        List<? extends Property> properties = definition.getProperties()
            .orElseThrow();

        Property.DynamicPropertiesProperty dynamicPropertiesProperty = properties.stream()
            .filter(property -> property instanceof Property.DynamicPropertiesProperty)
            .map(property -> (Property.DynamicPropertiesProperty) property)
            .findFirst()
            .orElseThrow();

        return dynamicPropertiesProperty.getDynamicPropertiesDataSource()
            .flatMap(PropertiesDataSource::getPropertiesFunction)
            .orElseThrow();
    }

    private OutputFunction getOutputFunction() {
        return definition.getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(function -> (OutputFunction) function)
            .orElseThrow();
    }
}
