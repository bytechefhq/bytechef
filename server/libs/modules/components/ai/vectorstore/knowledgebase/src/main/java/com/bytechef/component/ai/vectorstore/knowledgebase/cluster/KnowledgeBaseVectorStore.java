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

package com.bytechef.component.ai.vectorstore.knowledgebase.cluster;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.createVectorStore;

import com.bytechef.component.ai.vectorstore.cluster.AbstractVectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Knowledge Base VectorStore cluster element for AI agent integration.
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseVectorStore {

    private KnowledgeBaseVectorStore() {
    }

    public static ClusterElementDefinition<VectorStoreFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService, VectorStore vectorStore) {

        return AbstractVectorStore.of("Knowledge Base", createVectorStore(vectorStore),
            clusterElementDefinitionService);
    }
}
