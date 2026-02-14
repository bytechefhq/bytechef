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

package com.bytechef.platform.component.handler.loader;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.component.definition.ActionDefinitionWrapper;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OpenApiComponentHandlerLoaderTest {

    @Test
    void testPerformFunctionFunctionCreatesPerformFromActionDefinition() {
        ActionDefinition actionDefinition = action("testAction")
            .properties(string("param1").required(true))
            .output(outputSchema(string()));

        PerformFunction performFunction = OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(
            actionDefinition);

        assertNotNull(performFunction);
    }

    @Test
    void testToolWithActionDefinitionWrapperHasNonNullElement() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action")
            .description("Test Description")
            .properties(string("param1"));

        PerformFunction performFunction = OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(
            actionDefinition);

        ActionDefinitionWrapper wrappedAction = new ActionDefinitionWrapper(actionDefinition, performFunction);

        ClusterElementDefinition<ToolFunction> toolDefinition = tool(wrappedAction);

        assertNotNull(toolDefinition.getElement());
    }

    @Test
    void testToolFromActionWithoutPerformHasNullElement() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action");

        ClusterElementDefinition<ToolFunction> toolDefinition = tool(actionDefinition);

        assertNull(toolDefinition.getElement());
    }

    @Test
    void testToolFromWrappedActionPreservesName() {
        ActionDefinition actionDefinition = action("myAction")
            .title("My Action")
            .description("My Description")
            .properties(string("param1"));

        PerformFunction performFunction = OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(
            actionDefinition);

        ActionDefinitionWrapper wrappedAction = new ActionDefinitionWrapper(actionDefinition, performFunction);

        ClusterElementDefinition<ToolFunction> toolDefinition = tool(wrappedAction);

        assertEquals("myAction", toolDefinition.getName());
    }

    @Test
    void testToolFromWrappedActionPreservesProperties() {
        ActionDefinition actionDefinition = action("testAction")
            .properties(string("param1"), string("param2"));

        PerformFunction performFunction = OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(
            actionDefinition);

        ActionDefinitionWrapper wrappedAction = new ActionDefinitionWrapper(actionDefinition, performFunction);

        ClusterElementDefinition<ToolFunction> toolDefinition = tool(wrappedAction);

        Optional<List<? extends Property>> propertiesOptional =
            toolDefinition.getProperties();

        assertTrue(propertiesOptional.isPresent());

        List<? extends Property> properties = propertiesOptional.get();

        assertEquals(2, properties.size());
    }

    @Test
    void testComponentWithClusterElementsFromActionsWithoutPerform() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action")
            .description("Test Description");

        ComponentDefinition componentDefinition = component("testComponent")
            .title("Test Component")
            .version(1)
            .actions(actionDefinition)
            .clusterElements(tool(actionDefinition));

        Optional<List<ClusterElementDefinition<?>>> clusterElementsOptional = componentDefinition.getClusterElements();

        assertTrue(clusterElementsOptional.isPresent());
        List<ClusterElementDefinition<?>> clusterElementDefinitions = clusterElementsOptional.get();

        assertEquals(1, clusterElementDefinitions.size());

        ClusterElementDefinition<?> clusterElement = clusterElementDefinitions.getFirst();

        assertNull(clusterElement.getElement());
        assertEquals("testAction", clusterElement.getName());
    }

    @Test
    void testComponentWithClusterElementsFromActionsWithPerform() {
        ActionDefinition actionDefinition = action("testAction")
            .title("Test Action")
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> "result");

        ComponentDefinition componentDefinition = component("testComponent")
            .title("Test Component")
            .version(1)
            .actions(actionDefinition)
            .clusterElements(tool(actionDefinition));

        Optional<List<ClusterElementDefinition<?>>> clusterElementsOptional = componentDefinition.getClusterElements();

        assertTrue(clusterElementsOptional.isPresent());

        List<ClusterElementDefinition<?>> clusterElements = clusterElementsOptional.get();

        assertEquals(1, clusterElements.size());

        ClusterElementDefinition<?> clusterElement = clusterElements.getFirst();

        assertNotNull(clusterElement.getElement());
    }
}
