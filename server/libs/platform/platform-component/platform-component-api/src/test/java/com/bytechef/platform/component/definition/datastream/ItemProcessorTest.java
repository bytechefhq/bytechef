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

package com.bytechef.platform.component.definition.datastream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ItemProcessorTest {

    @Test
    void testProcessorClusterElementType() {
        ClusterElementType processorType = ItemProcessor.PROCESSOR;

        assertNotNull(processorType);
        assertEquals("PROCESSOR", processorType.name());
        assertEquals("processor", processorType.key());
        assertEquals("Processor", processorType.label());
        assertFalse(processorType.multipleElements());
        assertFalse(processorType.required());
    }

    @Test
    void testProcessMethod() throws Exception {
        ItemProcessor<String, String> processor = new ItemProcessor<>() {

            @Override
            public Map<String, String> process(
                Map<String, String> item, Parameters inputParameters, Parameters connectionParameters,
                ClusterElementContext context) {

                Map<String, String> result = new HashMap<>();

                for (Map.Entry<String, String> entry : item.entrySet()) {
                    result.put(entry.getKey()
                        .toUpperCase(),
                        entry.getValue()
                            .toUpperCase());
                }

                return result;
            }
        };

        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        Map<String, String> inputItem = Map.of("name", "john", "city", "boston");

        Map<String, String> result = processor.process(inputItem, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("JOHN", result.get("NAME"));
        assertEquals("BOSTON", result.get("CITY"));
    }

    @Test
    void testProcessWithDifferentTypes() throws Exception {
        ItemProcessor<Integer, String> processor = new ItemProcessor<>() {

            @Override
            public Map<String, Integer> process(
                Map<String, String> item, Parameters inputParameters, Parameters connectionParameters,
                ClusterElementContext context) {

                Map<String, Integer> result = new HashMap<>();

                for (Map.Entry<String, String> entry : item.entrySet()) {
                    result.put(entry.getKey(), entry.getValue()
                        .length());
                }

                return result;
            }
        };

        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        Map<String, String> inputItem = Map.of("name", "john", "city", "boston");

        Map<String, Integer> result = processor.process(inputItem, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals(4, result.get("name"));
        assertEquals(6, result.get("city"));
    }
}
