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

package com.bytechef.component.script.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.cluster.datastream.ScriptJavaScriptItemProcessor;
import com.bytechef.component.script.cluster.datastream.definition.ScriptClusterElementDefinition;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.datastream.ItemProcessor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ScriptJavaScriptItemProcessorTest {

    @Test
    void testOfCreatesValidDefinition() {
        PolyglotEngine polyglotEngine = mock(PolyglotEngine.class);

        ScriptClusterElementDefinition definition = ScriptJavaScriptItemProcessor.of(polyglotEngine);

        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("javaScript");
        assertThat(definition.getTitle()).contains("JavaScript");
        assertThat(definition.getDescription()).contains("Transforms data stream items using custom JavaScript code.");
        assertThat(definition.getType()).isEqualTo(ItemProcessor.PROCESSOR);
    }

    @Test
    void testOfElementTransformsItemWithJavaScript() throws Exception {
        ScriptClusterElementDefinition definition = ScriptJavaScriptItemProcessor.of(
            new PolyglotEngine(mock(ApplicationContext.class)));

        ItemProcessor<Object, Object> itemProcessor = definition.getElement();

        assertThat(itemProcessor).isNotNull();

        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                "script",
                "function perform(input, context) {\n\treturn {name: input.item.name.toUpperCase(), size: 2};\n}"));
        ClusterElementContext context = mock(
            ClusterElementContext.class, withSettings().extraInterfaces(ClusterElementContextAware.class));

        Map<String, Object> result = itemProcessor.process(
            Map.of("name", "john"), inputParameters, ParametersFactory.create(Map.of()), context);

        assertThat(result).containsEntry("name", "JOHN");
        assertThat(result).containsKey("size");
    }

    @Test
    void testOfReturnsDefinitionWithProperties() {
        PolyglotEngine polyglotEngine = mock(PolyglotEngine.class);

        ScriptClusterElementDefinition definition = ScriptJavaScriptItemProcessor.of(polyglotEngine);

        assertThat(definition.getProperties()).hasSize(1);
        assertThat(definition.getProperties()
            .getFirst()
            .getName()).isEqualTo("script");
    }

    @Test
    void testOfDefinitionHasScriptProperty() {
        PolyglotEngine polyglotEngine = mock(PolyglotEngine.class);

        ScriptClusterElementDefinition definition = ScriptJavaScriptItemProcessor.of(polyglotEngine);

        assertThat(definition.getProperties()).isNotEmpty();
        assertThat(definition.getProperties()
            .getFirst()
            .getName()).isEqualTo("script");
    }
}
