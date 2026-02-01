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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ClusterElementContextAware.ClusterElementFunction;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementResolverFunctionTest {

    @Test
    void testResolveReturnsExpectedResult() {
        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        ClusterElementResolverFunction resolver = new ClusterElementResolverFunction() {

            @Override
            public <T> T resolve(
                ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction) {

                if ("SOURCE".equals(clusterElementType.name())) {
                    return clusterElementFunction.apply("element", inputParameters, connectionParameters, context);
                }

                return null;
            }
        };

        ClusterElementType sourceType = new ClusterElementType("SOURCE", "source", "Source");
        ClusterElementType otherType = new ClusterElementType("OTHER", "other", "Other");

        String result = resolver.resolve(
            sourceType,
            (clusterElement, inputParams, connectionParams, ctx) -> "source-result");

        assertEquals("source-result", result);

        String otherResult = resolver.resolve(
            otherType,
            (clusterElement, inputParams, connectionParams, ctx) -> "other-result");

        assertNull(otherResult);
    }

    @Test
    void testResolveWithDifferentReturnTypes() {
        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        ClusterElementResolverFunction resolver = new ClusterElementResolverFunction() {

            @Override
            public <T> T resolve(
                ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction) {

                return clusterElementFunction.apply("element", inputParameters, connectionParameters, context);
            }
        };

        ClusterElementType sourceType = new ClusterElementType("SOURCE", "source", "Source");

        String stringResult = resolver.resolve(
            sourceType,
            (clusterElement, inputParams, connectionParams, ctx) -> "string");

        assertEquals("string", stringResult);

        Integer intResult = resolver.resolve(
            sourceType,
            (clusterElement, inputParams, connectionParams, ctx) -> 42);

        assertEquals(42, intResult);

        Boolean boolResult = resolver.resolve(
            sourceType,
            (clusterElement, inputParams, connectionParams, ctx) -> true);

        assertEquals(true, boolResult);
    }

    @Test
    void testResolveProvidesClusterElementToFunction() {
        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);
        String expectedElement = "testElement";

        ClusterElementResolverFunction resolver = new ClusterElementResolverFunction() {

            @Override
            public <T> T resolve(
                ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction) {

                return clusterElementFunction.apply(expectedElement, inputParameters, connectionParameters, context);
            }
        };

        ClusterElementType type = new ClusterElementType("TEST", "test", "Test");

        String result = resolver.resolve(
            type,
            (clusterElement, inputParams, connectionParams, ctx) -> "Element: " + clusterElement);

        assertEquals("Element: testElement", result);
    }
}
