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

package com.bytechef.component.script.datastream.definition;

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ScriptClusterElementDefinitionTest {

    private ScriptClusterElementDefinition scriptClusterElementDefinition;
    private PolyglotEngine polyglotEngine;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;

    @BeforeEach
    void setUp() {
        polyglotEngine = mock(PolyglotEngine.class);
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(
            ClusterElementContext.class,
            withSettings().extraInterfaces(ClusterElementContextAware.class));

        ClusterElementDefinition<?> clusterElementDefinition = clusterElement("test")
            .title("Test")
            .description("Test processor");

        scriptClusterElementDefinition = new ScriptClusterElementDefinition(
            clusterElementDefinition, "js", polyglotEngine);
    }

    @Test
    void testProcessDelegatestoPolyglotEngine() {
        Map<String, Object> item = Map.of("name", "John", "age", 30);
        Map<String, Object> expectedResult = Map.of("name", "John Doe", "age", 31);

        when(polyglotEngine.execute(eq("js"), any(Parameters.class), eq(null), any(ClusterElementContextAware.class)))
            .thenReturn(expectedResult);

        Map<String, Object> result = scriptClusterElementDefinition.process(
            item, inputParameters, connectionParameters, context);

        assertThat(result).isEqualTo(expectedResult);

        verify(polyglotEngine).execute(eq("js"), any(Parameters.class), eq(null),
            any(ClusterElementContextAware.class));
    }

    @Test
    void testProcessPassesItemInInputParameters() {
        Map<String, Object> item = Map.of("id", 1, "value", "test");
        Map<String, Object> expectedResult = Map.of("id", 1, "value", "processed");

        ArgumentCaptor<Parameters> parametersCaptor = ArgumentCaptor.forClass(Parameters.class);

        when(polyglotEngine.execute(eq("js"), parametersCaptor.capture(), eq(null),
            any(ClusterElementContextAware.class)))
                .thenReturn(expectedResult);

        scriptClusterElementDefinition.process(item, inputParameters, connectionParameters, context);

        Parameters capturedParameters = parametersCaptor.getValue();

        assertThat(capturedParameters).isNotNull();
    }

    @Test
    void testProcessReturnsNullWhenEngineReturnsNull() {
        Map<String, Object> item = Map.of("data", "test");

        when(polyglotEngine.execute(eq("js"), any(Parameters.class), eq(null), any(ClusterElementContextAware.class)))
            .thenReturn(null);

        Map<String, Object> result = scriptClusterElementDefinition.process(
            item, inputParameters, connectionParameters, context);

        assertThat(result).isNull();
    }

    @Test
    void testProcessWithEmptyItem() {
        Map<String, Object> item = Map.of();
        Map<String, Object> expectedResult = Map.of("processed", true);

        when(polyglotEngine.execute(eq("js"), any(Parameters.class), eq(null), any(ClusterElementContextAware.class)))
            .thenReturn(expectedResult);

        Map<String, Object> result = scriptClusterElementDefinition.process(
            item, inputParameters, connectionParameters, context);

        assertThat(result).isEqualTo(expectedResult);
    }
}
