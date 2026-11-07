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

package com.bytechef.component.ai.vectorstore.knowledgebase.util;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.ADDITIONAL_METADATA;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_FILTER;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.IS_MULTIPLE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.TAG_NAMES;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.VECTOR_STORE;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader.Chunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.vectorstore.KnowledgeBaseVectorStoreWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader, KnowledgeBaseService knowledgeBaseService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        VectorStore kbVectorStore = createVectorStore(knowledgeBaseDocumentLoader, vectorStore);

        return ComponentDsl.<VectorStoreFunction>clusterElement(VECTOR_STORE)
            .title("Knowledge Base VectorStore")
            .description("Knowledge Base VectorStore.")
            .type(VectorStoreFunction.VECTOR_STORE)
            .properties(
                ComponentDsl.integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .resourceReference(ResourceType.KNOWLEDGE_BASE)
                    .description("The knowledge base to retrieve documents from.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                array(TAG_NAMES)
                    .label("Tags")
                    .description(
                        "Filter results by tags. Documents with ANY of the selected tags will be returned (OR logic).")
                    .items(string())
                    .options(KnowledgeBaseOptionsUtils.tagOptions(knowledgeBaseDocumentTagService))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_ID)
                    .required(false))
            .object(() -> (
                inputParameters, connectionParameters, extensions,
                componentConnections) -> kbVectorStore.createVectorStore(
                    inputParameters, ParametersFactory.create(connectionParameters), null));
    }

    public static VectorStore createVectorStore(org.springframework.ai.vectorstore.VectorStore vectorStore) {
        return new VectorStoreImpl(null, vectorStore);
    }

    public static VectorStore createVectorStore(
        KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader,
        org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return new VectorStoreImpl(knowledgeBaseDocumentLoader, vectorStore);
    }

    private static List<Chunk> readChunks(
        DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

        List<Document> documents = documentReader.read();

        for (DocumentTransformer documentTransformer : documentTransformers) {
            documents = documentTransformer.transform(documents);
        }

        return documents.stream()
            .map(document -> new Chunk(document.getId(), document.getText(), document.getMetadata()))
            .toList();
    }

    private static class VectorStoreImpl implements VectorStore {

        private final KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;
        private final org.springframework.ai.vectorstore.VectorStore vectorStore;

        public VectorStoreImpl(
            KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader,
            org.springframework.ai.vectorstore.VectorStore vectorStore) {

            this.knowledgeBaseDocumentLoader = knowledgeBaseDocumentLoader;
            this.vectorStore = vectorStore;
        }

        @Override
        public org.springframework.ai.vectorstore.VectorStore createVectorStore(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);
            List<String> tagNames = inputParameters.getList(TAG_NAMES, String.class);

            return new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId, tagNames);
        }

        @Override
        public void load(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            List<Chunk> chunks = readChunks(documentReader, documentTransformers);

            Map<String, Object> additionalMetadata = getAdditionalMetadata(inputParameters);

            knowledgeBaseDocumentLoader.load(
                inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID), inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_ID),
                KnowledgeBaseDocumentLoader.deriveDocumentName(chunks, additionalMetadata), chunks, additionalMetadata);
        }

        @Override
        public void update(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

            if (Boolean.TRUE.equals(inputParameters.getBoolean(IS_MULTIPLE))) {
                updateMultiple(
                    inputParameters, connectionParameters, embeddingModel, knowledgeBaseId, documentReader,
                    documentTransformers);
            } else {
                knowledgeBaseDocumentLoader.update(
                    knowledgeBaseId, inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_ID),
                    inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID),
                    readChunks(documentReader, documentTransformers), getAdditionalMetadata(inputParameters));
            }
        }

        private static Map<String, Object> getAdditionalMetadata(Parameters inputParameters) {
            Map<String, Object> additionalMetadata = inputParameters.getMap(
                ADDITIONAL_METADATA, new TypeReference<>() {});

            return additionalMetadata == null ? Map.of() : additionalMetadata;
        }

        private void updateMultiple(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            Long knowledgeBaseId, DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            List<Map<String, Object>> metadata = inputParameters.getList(METADATA_FILTER, new TypeReference<>() {});

            Map<String, Object> deleteParametersMap = new HashMap<>();

            deleteParametersMap.put(KNOWLEDGE_BASE_ID, knowledgeBaseId);
            deleteParametersMap.put(METADATA_FILTER, metadata);

            delete(ParametersFactory.create(deleteParametersMap), connectionParameters, embeddingModel);

            load(
                ParametersFactory.create(deleteParametersMap), connectionParameters, embeddingModel, documentReader,
                documentTransformers);
        }

        @Override
        public List<Document> search(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);
            List<String> tagNames = inputParameters.getList(TAG_NAMES, String.class);

            org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId, tagNames);

            return wrappedVectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
        }
    }
}
