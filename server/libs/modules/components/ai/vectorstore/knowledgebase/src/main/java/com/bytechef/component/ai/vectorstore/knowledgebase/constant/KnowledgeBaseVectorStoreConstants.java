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

package com.bytechef.component.ai.vectorstore.knowledgebase.constant;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStoreWrapper;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Ivica Cardic
 */
public final class KnowledgeBaseVectorStoreConstants {

    public static final String KNOWLEDGE_BASE = "knowledgeBase";
    public static final String KNOWLEDGE_BASE_ID = "knowledgeBaseId";
    public static final String METADATA_TAG_IDS = "tag_ids";
    public static final String QUERY = "query";
    public static final String SIMILARITY_THRESHOLD = "similarityThreshold";
    public static final String TAG_IDS = "tagIds";
    public static final String TOP_K = "topK";

    private KnowledgeBaseVectorStoreConstants() {
    }

    public static VectorStore createVectorStore(org.springframework.ai.vectorstore.VectorStore vectorStore) {
        return new VectorStore() {

            @Override
            public org.springframework.ai.vectorstore.VectorStore createVectorStore(
                Parameters connectionParameters, EmbeddingModel embeddingModel) {

                Long knowledgeBaseId = connectionParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

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
