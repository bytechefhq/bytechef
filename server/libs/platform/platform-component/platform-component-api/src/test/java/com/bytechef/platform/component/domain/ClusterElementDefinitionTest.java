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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementDefinitionTest {

    @Test
    void testWithPrependedProperties() {
        ClusterElementDefinition clusterElementDefinition = createClusterElementDefinition();

        ClusterElementDefinition result = clusterElementDefinition.withPrependedProperties(
            List.of(Property.toProperty(string("bar")), Property.toProperty(string("baz"))));

        assertEquals(List.of("bar", "baz", "foo"), getPropertyNames(result));
    }

    @Test
    void testWithPrependedPropertiesLeavesTheSourceDefinitionUnchanged() {
        ClusterElementDefinition clusterElementDefinition = createClusterElementDefinition();

        ClusterElementDefinition result = clusterElementDefinition.withPrependedProperties(
            List.of(Property.toProperty(string("bar"))));

        assertEquals(List.of("bar", "foo"), getPropertyNames(result));
        assertEquals(List.of("foo"), getPropertyNames(clusterElementDefinition));
    }

    @Test
    void testWithPrependedPropertiesCopiesTheRemainingFields() {
        ClusterElementDefinition clusterElementDefinition = createClusterElementDefinition();

        ClusterElementDefinition result = clusterElementDefinition.withPrependedProperties(
            List.of(Property.toProperty(string("bar"))));

        assertEquals("comp", result.getComponentName());
        assertEquals(1, result.getComponentVersion());
        assertEquals("test", result.getName());
        assertEquals("Test", result.getTitle());
        assertEquals("A test element", result.getDescription());
        assertEquals(TOOLS, result.getType());
    }

    @Test
    void testIsTestableForAToolFunction() {
        assertTrue(createClusterElementDefinition(mock(ToolFunction.class)).isTestable());
    }

    @Test
    void testIsTestableIsFalseForAToolCallbackProviderFunction() {
        assertFalse(createClusterElementDefinition(mock(ToolCallbackProviderFunction.class)).isTestable());
    }

    @Test
    void testIsTestableIsFalseForAMultipleConnectionsToolCallbackProviderFunction() {
        ClusterElementDefinition clusterElementDefinition = createClusterElementDefinition(
            mock(MultipleConnectionsToolCallbackProviderFunction.class));

        assertFalse(clusterElementDefinition.isTestable());
    }

    @Test
    void testWithPrependedPropertiesKeepsTestable() {
        ClusterElementDefinition clusterElementDefinition = createClusterElementDefinition(
            mock(ToolCallbackProviderFunction.class));

        ClusterElementDefinition result = clusterElementDefinition.withPrependedProperties(
            List.of(Property.toProperty(string("bar"))));

        assertFalse(result.isTestable());
    }

    private static ClusterElementDefinition createClusterElementDefinition(Object element) {
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            clusterElement("test")
                .title("Test")
                .type(TOOLS)
                .object(() -> element);

        return new ClusterElementDefinition(clusterElementDefinition, "comp", 1, "icon");
    }

    private static ClusterElementDefinition createClusterElementDefinition() {
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            clusterElement("test")
                .title("Test")
                .description("A test element")
                .type(TOOLS)
                .properties(string("foo"));

        return new ClusterElementDefinition(clusterElementDefinition, "comp", 1, "icon");
    }

    private static List<String> getPropertyNames(ClusterElementDefinition clusterElementDefinition) {
        return clusterElementDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();
    }
}
