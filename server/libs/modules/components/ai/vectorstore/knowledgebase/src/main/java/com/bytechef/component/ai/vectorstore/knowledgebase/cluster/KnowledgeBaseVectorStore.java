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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.cluster.VectorStoreDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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

    private static final String METADATA_ENVIRONMENT = "environment";
    private static final String METADATA_KNOWLEDGE_BASE_DOCUMENT_ID = "knowledge_base_document_id";
    private static final String METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID = "knowledge_base_document_chunk_id";

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

    public static VectorStore createVectorStore(
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService) {

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

                knowledgeBaseDocument =
                    knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

                long knowledgeBaseDocumentId = knowledgeBaseDocument.getId();

                try {
                    for (Document document : documents) {
                        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

                        chunk.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);

                        chunk = knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(chunk);

                        Long chunkId = chunk.getId();

                        Map<String, Object> metadata = new HashMap<>(document.getMetadata());

                        metadata.put(METADATA_ENVIRONMENT, knowledgeBase.getEnvironmentId());
                        metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId);
                        metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, chunkId);

                        String text = document.getText();

                        if (text != null) {
                            text = text.replace("\0", "");
                        }

                        Document enrichedDocument = new Document(document.getId(), text, metadata);

                        wrappedVectorStore.add(List.of(enrichedDocument));

                        chunk.setVectorStoreId(enrichedDocument.getId());

                        FileEntry chunkContent = knowledgeBaseFileStorage.storeChunkContent(chunkId, text);

                        chunk.setContent(chunkContent);

                        knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(chunk);
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

                org.springframework.ai.vectorstore.VectorStore wrappedVectorStore =
                    new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

                return wrappedVectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
            }
        };
    }

    private static String deriveDocumentName(List<Document> documents) {
        if (!documents.isEmpty()) {
            Object source = documents.getFirst()
                .getMetadata()
                .get("source");

            if (source != null) {
                return source.toString();
            }
        }

        return "Workflow Import";
    }
}
