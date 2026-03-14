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

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.QUERY;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.cluster.VectorStoreDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Knowledge Base VectorStore cluster element for AI agent integration.
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseVectorStore {

    private KnowledgeBaseVectorStore() {
    }

    public static ClusterElementDefinition<VectorStoreFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService,
        org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return VectorStoreDefinition.of(
            "Knowledge Base", createVectorStore(vectorStore), clusterElementDefinitionService);
    }

    public static VectorStore createVectorStore(org.springframework.ai.vectorstore.VectorStore vectorStore) {
        return new VectorStore() {

            @Override
            public org.springframework.ai.vectorstore.VectorStore createVectorStore(
                Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

                Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

                return new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);
            }

            @Override
            public void load(
                Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
                DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

                Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

                org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                    new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

                List<Document> documents = documentReader.read();

                for (DocumentTransformer documentTransformer : documentTransformers) {
                    documents = documentTransformer.transform(documents);
                }

                wrappedVectorStore.add(documents);
            }

            @Override
            public List<Document> search(
                Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

                Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

                org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                    new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

                return wrappedVectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
            }
        };
    }
}
