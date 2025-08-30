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

import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentJoinerFunction.DOCUMENT_JOINER;
import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentRetrieverFunction.DOCUMENT_RETRIEVER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryAugmenterFunction.QUERY_AUGMENTER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryExpanderFunction.QUERY_EXPANDER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryTransformerFunction.QUERY_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ModularRagComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(DOCUMENT_JOINER, DOCUMENT_RETRIEVER, QUERY_AUGMENTER, QUERY_EXPANDER, QUERY_TRANSFORMER);
    }
}
