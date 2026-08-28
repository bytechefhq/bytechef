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

package com.bytechef.component.ai.vectorstore.knowledgebase;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE_FULL_REPLACE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE_PARTIAL;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.SOURCE_ID;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.datastream.ItemWriter.DESTINATION;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseDeleteAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseLoadAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseSearchAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseUpdateAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseSearchTool;
import com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseUpdateTool;
import com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter;
import com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseVectorStore;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import com.bytechef.platform.owner.OwnerResolver;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Component handler for the internal Knowledge Base vector store.
 *
 * @author Ivica Cardic
 */
@Component(KNOWLEDGE_BASE + "_v1_ComponentHandler")
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public KnowledgeBaseComponentHandler(
        ClusterElementDefinitionService clusterElementDefinitionService,
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService,
        KnowledgeBaseSourceService knowledgeBaseSourceService,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore,
        ObjectProvider<OwnerResolver> ownerResolverProvider) {

        this.componentDefinition =
            new KnowledgeBaseVectorStoreComponentDefinitionImpl(
                clusterElementDefinitionService, knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentChunkService,
                knowledgeBaseDocumentService, knowledgeBaseDocumentTagService, knowledgeBaseFileStorage,
                knowledgeBaseService, knowledgeBaseSourceService, vectorStore, ownerResolverProvider);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class KnowledgeBaseVectorStoreComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public KnowledgeBaseVectorStoreComponentDefinitionImpl(
            ClusterElementDefinitionService clusterElementDefinitionService,
            KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
            KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
            KnowledgeBaseDocumentService knowledgeBaseDocumentService,
            KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
            KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService,
            KnowledgeBaseSourceService knowledgeBaseSourceService, VectorStore vectorStore,
            ObjectProvider<OwnerResolver> ownerResolverProvider) {

            super(
                component(KNOWLEDGE_BASE)
                    .title("Knowledge Base")
                    .description(
                        "Search ByteChef's internal knowledge base to retrieve relevant document chunks using " +
                            "semantic similarity search powered by vector embeddings.")
                    .icon("path:assets/knowledge-base.svg")
                    .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                    .actions(
                        KnowledgeBaseDeleteAction.of(
                            vectorStore, knowledgeBaseService, ownerResolverProvider),
                        KnowledgeBaseLoadAction.of(
                            vectorStore, clusterElementDefinitionService, knowledgeBaseDocumentChunkService,
                            knowledgeBaseDocumentService, knowledgeBaseFileStorage, knowledgeBaseService,
                            ownerResolverProvider),
                        KnowledgeBaseSearchAction.of(
                            vectorStore, knowledgeBaseService, knowledgeBaseDocumentTagService,
                            ownerResolverProvider),
                        KnowledgeBaseUpdateAction.of(
                            vectorStore, clusterElementDefinitionService, knowledgeBaseDocumentChunkFacade,
                            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
                            knowledgeBaseService,
                            ownerResolverProvider))
                    .clusterElements(
                        KnowledgeBaseSearchTool.of(
                            vectorStore, knowledgeBaseService, knowledgeBaseDocumentTagService,
                            ownerResolverProvider),
                        KnowledgeBaseUpdateTool.of(
                            vectorStore, knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentChunkService,
                            knowledgeBaseDocumentService, knowledgeBaseFileStorage, knowledgeBaseService,
                            ownerResolverProvider),
                        KnowledgeBaseVectorStore.of(
                            vectorStore, knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService,
                            knowledgeBaseFileStorage, knowledgeBaseService, knowledgeBaseDocumentTagService,
                            ownerResolverProvider),
                        writeAsDocumentClusterElement(
                            knowledgeBaseSourceService, knowledgeBaseDocumentService)));
        }

        private static ModifiableClusterElementDefinition<KnowledgeBaseItemWriter> writeAsDocumentClusterElement(
            KnowledgeBaseSourceService knowledgeBaseSourceService,
            KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

            return ComponentDsl.<KnowledgeBaseItemWriter>clusterElement("writeAsDocument")
                .title("Write as Knowledge Base Document")
                .description(
                    "Upsert source records into the target Knowledge Base as documents. " +
                        "FULL_REPLACE mode tombstones documents whose source_record_id is not seen this run; " +
                        "PARTIAL mode skips the tombstone sweep for backfills and partial-update workflows.")
                .type(DESTINATION)
                .object(() -> new KnowledgeBaseItemWriter(
                    knowledgeBaseSourceService, knowledgeBaseDocumentService))
                .properties(
                    integer(SOURCE_ID)
                        .label("Knowledge Base Source ID")
                        .description("ID of the KnowledgeBaseSource that owns this sync.")
                        .required(true),
                    string(MODE)
                        .label("Sync mode")
                        .description(
                            "FULL_REPLACE = tombstone records not seen this run (default); " +
                                "PARTIAL = leave unseen records untouched (use for backfills / partial updates).")
                        .options(
                            option(MODE_FULL_REPLACE, MODE_FULL_REPLACE),
                            option(MODE_PARTIAL, MODE_PARTIAL))
                        .defaultValue(MODE_FULL_REPLACE)
                        .required(false));
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(DOCUMENT_READER, DOCUMENT_TRANSFORMER);
        }

        @Override
        public Map<String, List<String>> getActionClusterElementTypes() {
            // LinkedHashMap rather than Map.of: Map.of randomises its iteration order per JVM run, so these keys
            // shuffle in the generated knowledgeBase_v1.json snapshot every time anyone regenerates it. Purely
            // cosmetic -- JsonFileAssert compares JSON objects order-insensitively -- but it makes each regeneration
            // produce a spurious diff. These override the VectorStoreComponentDefinition defaults, which are pinned
            // the same way for the same reason.
            Map<String, List<String>> actionClusterElementTypes = new LinkedHashMap<>();

            actionClusterElementTypes.put(DELETE, List.of());
            actionClusterElementTypes.put(LOAD, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name()));
            actionClusterElementTypes.put(SEARCH, List.of());
            actionClusterElementTypes.put(UPDATE, List.of(DOCUMENT_READER.name(), DOCUMENT_TRANSFORMER.name()));

            return Collections.unmodifiableMap(actionClusterElementTypes);
        }

        @Override
        public Map<String, List<String>> getClusterElementClusterElementTypes() {
            // See getActionClusterElementTypes above -- same reason.
            Map<String, List<String>> clusterElementClusterElementTypes = new LinkedHashMap<>();

            clusterElementClusterElementTypes.put(VECTOR_STORE, List.of());
            clusterElementClusterElementTypes.put(SEARCH, List.of());

            return Collections.unmodifiableMap(clusterElementClusterElementTypes);
        }
    }
}
