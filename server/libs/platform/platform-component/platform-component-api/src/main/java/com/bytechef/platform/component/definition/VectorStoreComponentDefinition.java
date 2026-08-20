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

import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;
import static com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction.EMBEDDING;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface VectorStoreComponentDefinition extends ClusterRootComponentDefinition {

    /**
     *
     */
    String LOAD = "load";

    /**
     *
     */
    String SEARCH = "search";

    /**
     *
     */
    String DELETE = "delete";

    /**
     *
     */
    String UPDATE = "update";

    /**
     *
     */
    String VECTOR_STORE = "vectorStore";

    /**
     * Provides a list of cluster element types associated with the component definition.
     *
     * @return a list of {@code ClusterElementType} instances representing the cluster elements such as DOCUMENT_READER,
     *         DOCUMENT_TRANSFORMER, and EMBEDDING.
     */
    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(DOCUMENT_READER, DOCUMENT_TRANSFORMER, EMBEDDING);
    }

    /**
     * Retrieves a mapping of actions to their associated cluster element types.
     *
     * @return a map where the keys are action names (e.g., "load", "search") and the values are lists of cluster
     *         element type names (e.g., "DOCUMENT_READER", "DOCUMENT_TRANSFORMER", "EMBEDDING") corresponding to each
     *         action.
     */
    @Override
    default Map<String, List<String>> getActionClusterElementTypes() {
        // LinkedHashMap rather than Map.of: Map.of randomises its iteration order per JVM run, so these keys shuffle
        // in the generated definition snapshot of every vector-store component every time anyone regenerates them.
        // Purely cosmetic -- JsonFileAssert compares JSON objects order-insensitively -- but it turns each
        // regeneration into a spurious multi-file diff.
        Map<String, List<String>> actionClusterElementTypes = new LinkedHashMap<>();

        actionClusterElementTypes.put(DELETE, List.of(EMBEDDING.name()));
        actionClusterElementTypes.put(
            LOAD, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name(), EMBEDDING.name()));
        actionClusterElementTypes.put(SEARCH, List.of(EMBEDDING.name()));
        actionClusterElementTypes.put(
            UPDATE, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name(), EMBEDDING.name()));

        return Collections.unmodifiableMap(actionClusterElementTypes);
    }

    /**
     * Retrieves a mapping of root cluster element identifiers to their associated cluster element types.
     *
     * @return a map where the keys are cluster element identifiers (e.g., VECTOR_STORE, SEARCH) and the values are
     *         lists of cluster element type names (e.g., EMBEDDING) associated with each identifier.
     */
    @Override
    default Map<String, List<String>> getClusterElementClusterElementTypes() {
        // See getActionClusterElementTypes above -- same reason.
        Map<String, List<String>> clusterElementClusterElementTypes = new LinkedHashMap<>();

        clusterElementClusterElementTypes.put(VECTOR_STORE, List.of(EMBEDDING.name()));
        clusterElementClusterElementTypes.put(SEARCH, List.of(EMBEDDING.name()));

        return Collections.unmodifiableMap(clusterElementClusterElementTypes);
    }
}
