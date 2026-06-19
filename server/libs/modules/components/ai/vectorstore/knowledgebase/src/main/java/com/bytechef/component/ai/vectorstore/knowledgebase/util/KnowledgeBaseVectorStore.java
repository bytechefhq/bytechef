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
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_ENVIRONMENT_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_ID;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

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
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        VectorStore kbVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);

        return ComponentDsl.<VectorStoreFunction>clusterElement(VECTOR_STORE)
            .title("Knowledge Base VectorStore")
            .description("Knowledge Base VectorStore.")
            .type(VectorStoreFunction.VECTOR_STORE)
            .properties(
                ComponentDsl.integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
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
        return new VectorStoreImpl(null, null, null, null, vectorStore);
    }

    public static VectorStore createVectorStore(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService, org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return new VectorStoreImpl(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);
    }

    private static String deriveDocumentName(List<Document> documents) {
        if (!documents.isEmpty()) {
            Document document = documents.getFirst();

            Map<String, Object> metadata = document.getMetadata();

            Object filename = metadata.get("filename");

            if (filename != null) {
                return filename.toString();
            }
        }

        return "Workflow Import";
    }

    private static class VectorStoreImpl implements VectorStore {

        private final org.springframework.ai.vectorstore.VectorStore vectorStore;
        private final KnowledgeBaseService knowledgeBaseService;
        private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
        private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
        private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;

        public VectorStoreImpl(
            KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
            KnowledgeBaseDocumentService knowledgeBaseDocumentService,
            KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService,
            org.springframework.ai.vectorstore.VectorStore vectorStore) {

            this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
            this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
            this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
            this.knowledgeBaseService = knowledgeBaseService;
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

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

            KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);

            org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

            List<Document> documents = documentReader.read();

            for (DocumentTransformer documentTransformer : documentTransformers) {
                documents = documentTransformer.transform(documents);
            }

            KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

            knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeBaseDocument.setName(deriveDocumentName(documents));
            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

            knowledgeBaseDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

            long knowledgeBaseDocumentId = knowledgeBaseDocument.getId();

            try {
                for (Document document : documents) {
                    KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

                    knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);

                    knowledgeBaseDocumentChunk = knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(
                        knowledgeBaseDocumentChunk);

                    long knowledgeBaseDocumentChunkId = knowledgeBaseDocumentChunk.getId();

                    Map<String, Object> metadata = new HashMap<>(document.getMetadata());

                    metadata.put(METADATA_ENVIRONMENT_ID, knowledgeBase.getEnvironmentId());
                    metadata.put(METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);
                    metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId);
                    metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId);

                    Map<String, Object> userMetadata = inputParameters.getMap(
                        ADDITIONAL_METADATA, new TypeReference<>() {});

                    metadata.putAll(userMetadata);

                    String text = document.getText();

                    if (text != null) {
                        text = text.replace("\0", "");
                    }

                    Document enrichedDocument = new Document(document.getId(), text, metadata);

                    wrappedVectorStore.add(List.of(enrichedDocument));

                    knowledgeBaseDocumentChunk.setVectorStoreId(enrichedDocument.getId());

                    FileEntry chunkContent =
                        knowledgeBaseFileStorage.storeChunkContent(knowledgeBaseDocumentChunkId, text);

                    knowledgeBaseDocumentChunk.setContent(chunkContent);

                    knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunk);
                }

                knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_READY);

                knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);
            } catch (RuntimeException exception) {
                knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_ERROR);

                knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

                throw exception;
            }
        }

        @Override
        public void update(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

            if (Boolean.TRUE.equals(inputParameters.getBoolean(IS_MULTIPLE))) {
                updateMultiple(inputParameters, connectionParameters, embeddingModel, knowledgeBaseId,
                    documentReader, documentTransformers);
            } else {
                updateSingle(inputParameters, connectionParameters, embeddingModel, knowledgeBaseId,
                    documentReader, documentTransformers);
            }
        }

        private void updateMultiple(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            Long knowledgeBaseId, DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            List<Map<String, Object>> metadata = inputParameters.getList(METADATA_FILTER, new TypeReference<>() {});

            Map<String, Object> deleteParametersMap = new HashMap<>();

            deleteParametersMap.put(KNOWLEDGE_BASE_ID, knowledgeBaseId);
            deleteParametersMap.put(METADATA_FILTER, metadata);

            delete(ParametersFactory.create(deleteParametersMap), connectionParameters, embeddingModel);

            load(ParametersFactory.create(deleteParametersMap), connectionParameters, embeddingModel,
                documentReader, documentTransformers);
        }

        private void updateSingle(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            Long knowledgeBaseId, DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            Long knowledgeBaseDocumentId = inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_ID);
            Long knowledgeBaseDocumentChunkId = inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID);

            Map<String, Object> loadMetadata = new HashMap<>();

            if (knowledgeBaseDocumentChunkId != null) {
                FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

                Filter.Expression chunkFilter = filterBuilder
                    .eq(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId)
                    .build();

                List<Document> existingDocuments = new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId)
                    .similaritySearch(SearchRequest.builder()
                        .query(" ")
                        .topK(1)
                        .similarityThreshold(0.0)
                        .filterExpression(chunkFilter)
                        .build());

                if (!existingDocuments.isEmpty()) {
                    Map<String, Object> inheritedMetadata = new HashMap<>(existingDocuments.getFirst().getMetadata());

                    inheritedMetadata.remove(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID);

                    if (!inheritedMetadata.isEmpty()) {
                        loadMetadata.putAll(inheritedMetadata);
                    }
                }
            }

            Map<String, Object> additionalMetadata = inputParameters.getMap(ADDITIONAL_METADATA, Object.class);

            if (additionalMetadata != null && !additionalMetadata.isEmpty()) {
                loadMetadata.putAll(additionalMetadata);
            }

            Map<String, Object> deleteFilter = new HashMap<>();

            if (knowledgeBaseDocumentChunkId != null) {
                deleteFilter.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId);
            } else if (knowledgeBaseDocumentId != null) {
                deleteFilter.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId);
            }

            List<Map<String, Object>> metadataFilters = deleteFilter.isEmpty() ? List.of() : List.of(deleteFilter);

            Map<String, Object> deleteParametersMap = new HashMap<>();

            deleteParametersMap.put(KNOWLEDGE_BASE_ID, knowledgeBaseId);
            deleteParametersMap.put(ADDITIONAL_METADATA, metadataFilters);

            delete(ParametersFactory.create(deleteParametersMap), connectionParameters, embeddingModel);

            Map<String, Object> loadParametersMap = new HashMap<>();

            loadParametersMap.put(KNOWLEDGE_BASE_ID, knowledgeBaseId);
            loadParametersMap.put(ADDITIONAL_METADATA, loadMetadata);

            load(ParametersFactory.create(loadParametersMap), connectionParameters, embeddingModel,
                documentReader, documentTransformers);
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
