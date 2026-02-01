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

package com.bytechef.platform.component.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ClusterElementDefinition.ProcessErrorResponseFunction;
import com.bytechef.component.definition.ClusterElementDefinition.WorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AbstractClusterElementDefinitionWrapperTest {

    @Test
    void testWrapperDelegatesAllGetters() {
        ClusterElementDefinition<?> mockDefinition = createMockDefinition();

        TestWrapper wrapper = new TestWrapper(mockDefinition);

        assertEquals("Test Element", wrapper.getName());
        assertEquals("test", wrapper.getType()
            .key());
        assertEquals("Test Description", wrapper.getDescription()
            .orElse(null));
        assertEquals("Test Title", wrapper.getTitle()
            .orElse(null));
        assertNotNull(wrapper.getHelp()
            .orElse(null));
        assertNotNull(wrapper.getOutputDefinition()
            .orElse(null));
        assertNotNull(wrapper.getProcessErrorResponse()
            .orElse(null));
        assertNotNull(wrapper.getWorkflowNodeDescription()
            .orElse(null));
        assertNotNull(wrapper.getElement());
        assertEquals(2, wrapper.getProperties()
            .orElse(List.of())
            .size());
    }

    @Test
    void testWrapperWithNullOptionalFields() {
        ClusterElementDefinition<?> mockDefinition = mock(ClusterElementDefinition.class);
        ClusterElementType clusterElementType = new ClusterElementType("TEST", "test", "Test");

        when(mockDefinition.getName()).thenReturn("Minimal Element");
        when(mockDefinition.getType()).thenReturn(clusterElementType);
        when(mockDefinition.getElement()).thenReturn("element");
        when(mockDefinition.getDescription()).thenReturn(Optional.empty());
        when(mockDefinition.getTitle()).thenReturn(Optional.empty());
        when(mockDefinition.getHelp()).thenReturn(Optional.empty());
        when(mockDefinition.getOutputDefinition()).thenReturn(Optional.empty());
        when(mockDefinition.getProcessErrorResponse()).thenReturn(Optional.empty());
        when(mockDefinition.getWorkflowNodeDescription()).thenReturn(Optional.empty());
        when(mockDefinition.getProperties()).thenReturn(Optional.empty());

        TestWrapper wrapper = new TestWrapper(mockDefinition);

        assertEquals("Minimal Element", wrapper.getName());
        assertFalse(wrapper.getDescription()
            .isPresent());
        assertFalse(wrapper.getTitle()
            .isPresent());
        assertFalse(wrapper.getHelp()
            .isPresent());
        assertFalse(wrapper.getOutputDefinition()
            .isPresent());
        assertFalse(wrapper.getProcessErrorResponse()
            .isPresent());
        assertFalse(wrapper.getWorkflowNodeDescription()
            .isPresent());
        assertFalse(wrapper.getProperties()
            .isPresent());
    }

    @Test
    void testGetElementReturnsCorrectType() {
        ClusterElementDefinition<?> mockDefinition = mock(ClusterElementDefinition.class);
        ClusterElementType clusterElementType = new ClusterElementType("TEST", "test", "Test");
        String expectedElement = "testElement";

        when(mockDefinition.getName()).thenReturn("Element");
        when(mockDefinition.getType()).thenReturn(clusterElementType);
        when(mockDefinition.getElement()).thenReturn(expectedElement);
        when(mockDefinition.getDescription()).thenReturn(Optional.empty());
        when(mockDefinition.getTitle()).thenReturn(Optional.empty());
        when(mockDefinition.getHelp()).thenReturn(Optional.empty());
        when(mockDefinition.getOutputDefinition()).thenReturn(Optional.empty());
        when(mockDefinition.getProcessErrorResponse()).thenReturn(Optional.empty());
        when(mockDefinition.getWorkflowNodeDescription()).thenReturn(Optional.empty());
        when(mockDefinition.getProperties()).thenReturn(Optional.empty());

        TestWrapper wrapper = new TestWrapper(mockDefinition);

        assertEquals(expectedElement, wrapper.getElement());
    }

    @Test
    void testClusterElementType() {
        ClusterElementType typeWithAllFields = new ClusterElementType("NAME", "key", "Label", true, true);

        assertEquals("NAME", typeWithAllFields.name());
        assertEquals("key", typeWithAllFields.key());
        assertEquals("Label", typeWithAllFields.label());
        assertTrue(typeWithAllFields.multipleElements());
        assertTrue(typeWithAllFields.required());

        ClusterElementType typeWithRequired = new ClusterElementType("NAME", "key", "Label", true);

        assertEquals("NAME", typeWithRequired.name());
        assertFalse(typeWithRequired.multipleElements());
        assertTrue(typeWithRequired.required());

        ClusterElementType typeMinimal = new ClusterElementType("NAME", "key", "Label");

        assertEquals("NAME", typeMinimal.name());
        assertFalse(typeMinimal.multipleElements());
        assertFalse(typeMinimal.required());
    }

    @SuppressWarnings("unchecked")
    private ClusterElementDefinition<?> createMockDefinition() {
        ClusterElementDefinition<String> mockDefinition = mock(ClusterElementDefinition.class);
        ClusterElementType clusterElementType = new ClusterElementType("TEST", "test", "Test");
        Help mockHelp = mock(Help.class);
        OutputDefinition mockOutputDefinition = mock(OutputDefinition.class);
        ProcessErrorResponseFunction mockProcessErrorResponse = mock(ProcessErrorResponseFunction.class);
        WorkflowNodeDescriptionFunction mockWorkflowNodeDescription = mock(WorkflowNodeDescriptionFunction.class);
        Property mockProperty1 = mock(Property.class);
        Property mockProperty2 = mock(Property.class);

        when(mockDefinition.getName()).thenReturn("Test Element");
        when(mockDefinition.getType()).thenReturn(clusterElementType);
        when(mockDefinition.getDescription()).thenReturn(Optional.of("Test Description"));
        when(mockDefinition.getTitle()).thenReturn(Optional.of("Test Title"));
        when(mockDefinition.getElement()).thenReturn("element");
        when(mockDefinition.getHelp()).thenReturn(Optional.of(mockHelp));
        when(mockDefinition.getOutputDefinition()).thenReturn(Optional.of(mockOutputDefinition));
        when(mockDefinition.getProcessErrorResponse()).thenReturn(Optional.of(mockProcessErrorResponse));
        when(mockDefinition.getWorkflowNodeDescription()).thenReturn(Optional.of(mockWorkflowNodeDescription));
        when(mockDefinition.getProperties()).thenReturn(Optional.of(List.of(mockProperty1, mockProperty2)));

        return mockDefinition;
    }

    private static class TestWrapper extends AbstractClusterElementDefinitionWrapper<String> {

        TestWrapper(ClusterElementDefinition<?> clusterElementDefinition) {
            super(clusterElementDefinition);
        }
    }
}
