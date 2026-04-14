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

package com.bytechef.platform.component.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ClusterElementDefinitionServiceTest {

    private static final String COMPONENT_NAME = "testComponent";
    private static final int COMPONENT_VERSION = 1;

    @Mock
    private ComponentDefinitionRegistry componentDefinitionRegistry;

    @Mock
    private ContextFactory contextFactory;

    private ClusterElementDefinitionServiceImpl clusterElementDefinitionService;

    @BeforeEach
    void setUp() {
        clusterElementDefinitionService = new ClusterElementDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory);
    }

    @Test
    void testGetClusterElementDefinitionWithExactTypeName() {
        String clusterElementName = "openai";
        String clusterElementTypeName = "TOOLS";

        ClusterElementType toolsType = new ClusterElementType("TOOLS", "tools", "Tools");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createMatchableClusterElementDefinition(clusterElementName, toolsType);

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, clusterElementTypeName);

        assertNotNull(result);
        assertEquals(COMPONENT_NAME, result.getComponentName());
        assertEquals(COMPONENT_VERSION, result.getComponentVersion());
        assertEquals(clusterElementName, result.getName());
    }

    @Test
    void testGetClusterElementDefinitionWithChatMemoryTypeMatch() {
        String clusterElementName = "openai";
        String clusterElementTypeName = "CHAT_MEMORY";

        ClusterElementType chatMemoryType = new ClusterElementType("CHAT_MEMORY", "chatMemory", "Chat Memory");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createMatchableClusterElementDefinition(clusterElementName, chatMemoryType);

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, clusterElementTypeName);

        assertNotNull(result);
        assertEquals(clusterElementName, result.getName());
    }

    @Test
    void testGetClusterElementDefinitionWithVectorStoreTypeMatch() {
        String clusterElementName = "pinecone";
        String clusterElementTypeName = "VECTOR_STORE";

        ClusterElementType vectorStoreType = new ClusterElementType("VECTOR_STORE", "vectorStore", "Vector Store");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createMatchableClusterElementDefinition(clusterElementName, vectorStoreType);

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, clusterElementTypeName);

        assertNotNull(result);
        assertEquals(clusterElementName, result.getName());
    }

    @Test
    void testGetClusterElementDefinitionWithNonMatchingTypeThrowsException() {
        String clusterElementName = "openai";
        String clusterElementTypeName = "NONEXISTENT";

        ClusterElementType toolsType = new ClusterElementType("TOOLS", "tools", "Tools");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createFilterOnlyClusterElementDefinition(clusterElementName, toolsType);

        ComponentDefinition componentDefinition = createComponentDefinitionForError(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clusterElementDefinitionService.getClusterElementDefinition(
                COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, clusterElementTypeName));

        assertEquals(
            "Cluster element definition " + clusterElementName + " with type " + clusterElementTypeName +
                " not found in component " + COMPONENT_NAME,
            exception.getMessage());
    }

    @Test
    void testGetClusterElementDefinitionWithNonMatchingNameThrowsException() {
        String clusterElementName = "nonExistentElement";
        String clusterElementTypeName = "TOOLS";

        ClusterElementType toolsType = new ClusterElementType("TOOLS", "tools", "Tools");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createFilterOnlyClusterElementDefinition("openai", toolsType);

        ComponentDefinition componentDefinition = createComponentDefinitionForError(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clusterElementDefinitionService.getClusterElementDefinition(
                COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, clusterElementTypeName));

        assertEquals(
            "Cluster element definition " + clusterElementName + " with type " + clusterElementTypeName +
                " not found in component " + COMPONENT_NAME,
            exception.getMessage());
    }

    @Test
    void testExecuteToolDispatchesMultipleConnectionsToolFunction() throws Exception {
        String clusterElementName = "aiAgent";
        Object expectedResult = new Object();

        MultipleConnectionsToolFunction toolFunction = mock(MultipleConnectionsToolFunction.class);

        when(toolFunction.apply(any(), any(), any(), any(), any())).thenReturn(expectedResult);

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            mock(com.bytechef.component.definition.ClusterElementDefinition.class);

        when(elementDefinition.getName()).thenReturn(clusterElementName);
        when(elementDefinition.getElement()).thenAnswer(ignored -> toolFunction);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getClusterElements()).thenReturn(Optional.of(List.of(elementDefinition)));
        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementContext clusterElementContext = mock(ClusterElementContext.class);

        when(contextFactory.createClusterElementContext(
            eq(COMPONENT_NAME), eq(COMPONENT_VERSION), eq(clusterElementName), isNull(), anyBoolean()))
                .thenReturn(clusterElementContext);

        Map<String, ?> inputParameters = Map.of("userPrompt", "hi");
        Map<String, ?> extensions = Map.of("ext", "v");
        Map<String, ComponentConnection> componentConnections = Map.of();

        Object result = clusterElementDefinitionService.executeTool(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, inputParameters, extensions, componentConnections,
            true);

        assertSame(expectedResult, result);

        verify(toolFunction).apply(
            any(Parameters.class), any(Parameters.class), any(Parameters.class), eq(componentConnections),
            eq(clusterElementContext));
    }

    private com.bytechef.component.definition.ClusterElementDefinition<?> createMatchableClusterElementDefinition(
        String name, ClusterElementType type) {

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            mock(com.bytechef.component.definition.ClusterElementDefinition.class);

        when(elementDefinition.getName()).thenReturn(name);
        when(elementDefinition.getType()).thenReturn(type);
        when(elementDefinition.getDescription()).thenReturn(Optional.empty());
        when(elementDefinition.getHelp()).thenReturn(Optional.empty());
        when(elementDefinition.getTitle()).thenReturn(Optional.of(name));
        when(elementDefinition.getProperties()).thenReturn(Optional.empty());
        when(elementDefinition.getOutputDefinition()).thenReturn(Optional.empty());

        return elementDefinition;
    }

    private com.bytechef.component.definition.ClusterElementDefinition<?> createFilterOnlyClusterElementDefinition(
        String name, ClusterElementType type) {

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            mock(com.bytechef.component.definition.ClusterElementDefinition.class);

        when(elementDefinition.getName()).thenReturn(name);
        when(elementDefinition.getType()).thenReturn(type);

        return elementDefinition;
    }

    private ComponentDefinition createComponentDefinitionForMatch(
        List<com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions) {

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(COMPONENT_NAME);
        when(componentDefinition.getClusterElements()).thenReturn(Optional.of(clusterElementDefinitions));
        when(componentDefinition.getIcon()).thenReturn(Optional.empty());

        return componentDefinition;
    }

    private ComponentDefinition createComponentDefinitionForError(
        List<com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions) {

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        lenient().when(componentDefinition.getName())
            .thenReturn(COMPONENT_NAME);
        when(componentDefinition.getClusterElements()).thenReturn(Optional.of(clusterElementDefinitions));

        return componentDefinition;
    }
}
