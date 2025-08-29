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
        return Map.of(
            LOAD, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name(), EMBEDDING.name()),
            SEARCH, List.of(EMBEDDING.name()));
    }

    /**
     * Retrieves a mapping of root cluster element identifiers to their associated cluster element types.
     *
     * @return a map where the keys are cluster element identifiers (e.g., VECTOR_STORE) and the values are lists of
     *         cluster element type names (e.g., EMBEDDING) associated with each identifier.
     */
    @Override
    default Map<String, List<String>> getClusterElementClusterElementTypes() {
        return Map.of(VECTOR_STORE, List.of(EMBEDDING.name()));
    }
}
