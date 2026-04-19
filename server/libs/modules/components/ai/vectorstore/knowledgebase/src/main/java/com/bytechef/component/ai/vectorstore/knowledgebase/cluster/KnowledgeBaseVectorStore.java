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

import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_ENVIRONMENT_ID;
import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.TAG_IDS;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.VECTOR_STORE;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseTagService;
import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
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
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseTagService knowledgeBaseTagService) {

        VectorStore kbVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseDocumentTagService,
            knowledgeBaseFileStorage, knowledgeBaseService, vectorStore);

        return ComponentDsl.<VectorStoreFunction>clusterElement(VECTOR_STORE)
            .title("Knowledge Base VectorStore")
            .description("Knowledge Base VectorStore.")
            .type(VectorStoreFunction.VECTOR_STORE)
            .properties(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to retrieve documents from.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                array(TAG_IDS)
                    .label("Tags")
                    .description(
                        "Filter results by tags. Documents with ANY of the selected tags will be returned (OR logic).")
                    .items(integer())
                    .options(KnowledgeBaseOptionsUtils.tagOptions(knowledgeBaseTagService))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_ID)
                    .required(false))
            .object(() -> (
                inputParameters, connectionParameters, extensions,
                componentConnections) -> kbVectorStore.createVectorStore(
                    inputParameters, ParametersFactory.create(connectionParameters), null));
    }

    public static VectorStore createVectorStore(
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return new VectorStoreImpl(null, null, knowledgeBaseDocumentTagService, null, null, vectorStore);
    }

    public static VectorStore createVectorStore(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService, org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return new VectorStoreImpl(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, null, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);
    }

    public static VectorStore createVectorStore(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService,
        org.springframework.ai.vectorstore.VectorStore vectorStore) {

        return new VectorStoreImpl(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseDocumentTagService,
            knowledgeBaseFileStorage, knowledgeBaseService, vectorStore);
    }

    private static String deriveDocumentName(List<Document> documents) {
        if (!documents.isEmpty()) {
            Document document = documents.getFirst();

            Map<String, Object> metadata = document.getMetadata();

            Object source = metadata.get("source");

            if (source != null) {
                return source.toString();
            }
        }

        return "Workflow Import";
    }

    private static class VectorStoreImpl implements VectorStore {

        private final org.springframework.ai.vectorstore.VectorStore vectorStore;
        private final KnowledgeBaseService knowledgeBaseService;
        private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
        private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
        private final KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;
        private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;

        public VectorStoreImpl(
            KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
            KnowledgeBaseDocumentService knowledgeBaseDocumentService,
            KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
            KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService,
            org.springframework.ai.vectorstore.VectorStore vectorStore) {

            this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
            this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
            this.knowledgeBaseDocumentTagService = knowledgeBaseDocumentTagService;
            this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
            this.knowledgeBaseService = knowledgeBaseService;
            this.vectorStore = vectorStore;
        }

        @Override
        public org.springframework.ai.vectorstore.VectorStore createVectorStore(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);
            List<Long> tagIds = inputParameters.getList(TAG_IDS, Long.class);
            List<Long> documentIds = tagIds == null || tagIds.isEmpty()
                ? null
                : knowledgeBaseDocumentTagService.getDocumentIdsByTagIds(knowledgeBaseId, tagIds);

            return new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId, documentIds);
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
        public List<Document> search(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

            Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);
            List<Long> tagIds = inputParameters.getList(TAG_IDS, Long.class);
            List<Long> documentIds = tagIds == null || tagIds.isEmpty()
                ? null
                : knowledgeBaseDocumentTagService.getDocumentIdsByTagIds(knowledgeBaseId, tagIds);

            org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId, documentIds);

            return wrappedVectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
        }
    }
}
