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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ITERATION ORDER of both cluster-element-type maps, not just their contents.
 *
 * <p>
 * These two defaults are serialised verbatim into the generated definition snapshot of every vector-store component —
 * sixteen of them — and they used to be built with {@code Map.of}, whose iteration order the JVM randomises per run.
 * Every regeneration therefore reshuffled these keys across sixteen files at once. {@code JsonFileAssert} compares JSON
 * objects order-insensitively and so cannot catch that; these tests are the only thing that does.
 *
 * @author Ivica Cardic
 */
class VectorStoreComponentDefinitionTest {

    @Test
    void testActionClusterElementTypesIterationOrderIsDeterministic() {
        assertThat(vectorStoreComponentDefinition().getActionClusterElementTypes()
            .keySet())
                .containsExactly("delete", "load", "search", "update");
    }

    @Test
    void testClusterElementClusterElementTypesIterationOrderIsDeterministic() {
        assertThat(vectorStoreComponentDefinition().getClusterElementClusterElementTypes()
            .keySet())
                .containsExactly("vectorStore", "search");
    }

    /**
     * A map built by {@code Map.of} is also immutable, so pinning the order must not quietly cost that: a caller that
     * mutated a shared default would corrupt every other component's definition.
     */
    @Test
    void testMapsStayImmutable() {
        VectorStoreComponentDefinition vectorStoreComponentDefinition = vectorStoreComponentDefinition();

        assertThatMapIsImmutable(vectorStoreComponentDefinition.getActionClusterElementTypes());
        assertThatMapIsImmutable(vectorStoreComponentDefinition.getClusterElementClusterElementTypes());
    }

    private static void assertThatMapIsImmutable(Map<String, List<String>> map) {
        assertThatThrownBy(() -> map.put("x", List.of()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // CALLS_REAL_METHODS so the interface's own default methods run; the rest of ComponentDefinition is abstract and
    // irrelevant here.
    private static VectorStoreComponentDefinition vectorStoreComponentDefinition() {
        return mock(VectorStoreComponentDefinition.class, CALLS_REAL_METHODS);
    }
}
