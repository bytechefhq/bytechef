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

    @Override
    default List<ClusterElementType> getClusterElementType() {
        return List.of(DOCUMENT_READER, DOCUMENT_TRANSFORMER, EMBEDDING);
    }

    @Override
    default Map<String, List<String>> getActionClusterElementTypes() {
        return Map.of(
            LOAD, List.of(EMBEDDING.name()),
            SEARCH, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name(), EMBEDDING.name()));
    }
}
