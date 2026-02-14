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

package com.bytechef.component.definition;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDslToolTest {

    @Test
    void testToolWithPerformHasNonNullElement() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action")
            .description("Test Description")
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> "result");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertNotNull(toolDefinition.getElement());
    }

    @Test
    void testToolWithoutPerformHasNullElement() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action")
            .description("Test Description");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertNull(toolDefinition.getElement());
    }

    @Test
    void testToolPreservesActionName() {
        ActionDefinition actionDefinition = action("myAction")
            .title("My Action")
            .description("My Description");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertEquals("myAction", toolDefinition.getName());
    }

    @Test
    void testToolPreservesActionTitle() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Expected Title");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertTrue(toolDefinition.getTitle()
            .isPresent());
        assertEquals("Expected Title", toolDefinition.getTitle()
            .get());
    }

    @Test
    void testToolPreservesActionDescription() {
        ActionDefinition actionDefinition = action("testAction")
            .description("Expected Description");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertTrue(toolDefinition.getDescription()
            .isPresent());
        assertEquals("Expected Description", toolDefinition.getDescription()
            .get());
    }

    @Test
    void testToolPreservesActionProperties() {
        ActionDefinition actionDefinition = action("testAction")
            .properties(string("prop1"), string("prop2"));

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        Optional<List<? extends Property>> propertiesOptional = toolDefinition.getProperties();

        assertTrue(propertiesOptional.isPresent());

        List<? extends Property> properties = propertiesOptional.get();

        assertEquals(2, properties.size());
    }

    @Test
    void testToolWithoutPropertiesHasEmptyList() {
        ActionDefinition actionDefinition = action("testAction");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        Optional<List<? extends Property>> propertiesOptional = toolDefinition.getProperties();

        assertTrue(propertiesOptional.isPresent());

        List<? extends Property> properties = propertiesOptional.get();

        assertTrue(properties.isEmpty());
    }

    @Test
    void testToolHasToolsType() {
        ActionDefinition actionDefinition = action("testAction");

        ModifiableClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        ClusterElementType clusterElementType = toolDefinition.getType();

        assertNotNull(clusterElementType);
        assertEquals("TOOLS", clusterElementType.name());
    }
}
