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

package com.bytechef.component.ai.vectorstore.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseComponentHandlerTest {

    @Test
    void testGetDefinition() {
        JsonFileAssert.assertEquals(
            "definition/knowledgeBase_v1.json",
            new KnowledgeBaseComponentHandler(null, null, null, null, null, null, null, null, null, null)
                .getDefinition());
    }

    /**
     * Pins the ITERATION ORDER of the two cluster-element-type maps, not just their contents. They are serialised
     * verbatim into {@code knowledgeBase_v1.json} above and used to be built with {@code Map.of}, whose iteration order
     * the JVM randomises per run, so every regeneration reshuffled these keys. {@code JsonFileAssert} compares JSON
     * objects order-insensitively, which is exactly why {@code testGetDefinition} cannot catch this and these
     * assertions have to exist separately.
     *
     * <p>
     * Reached through {@code getDefinition()} because the definition class is a private inner class — which is also the
     * honest route: it is the object the snapshot is generated from.
     */
    @Test
    void testClusterElementTypeMapIterationOrderIsDeterministic() {
        ClusterRootComponentDefinition clusterRootComponentDefinition =
            (ClusterRootComponentDefinition) new KnowledgeBaseComponentHandler(
                null, null, null, null, null, null, null, null, null, null).getDefinition();

        assertThat(clusterRootComponentDefinition.getActionClusterElementTypes()
            .keySet())
                .containsExactly("delete", "load", "search", "update");
        assertThat(clusterRootComponentDefinition.getClusterElementClusterElementTypes()
            .keySet())
                .containsExactly("vectorStore", "search");
    }
}
