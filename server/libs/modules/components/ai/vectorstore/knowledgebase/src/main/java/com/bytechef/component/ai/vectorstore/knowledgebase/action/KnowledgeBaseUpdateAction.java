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

package com.bytechef.component.ai.vectorstore.knowledgebase.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.ADDITIONAL_METADATA;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_FILTER;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.CONTENT;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.IS_MULTIPLE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.UPDATE;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseOptionsUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentEnricherFunction;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentSplitterFunction;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;

/**
 * @author Marko Kriskovic
 */
public final class KnowledgeBaseUpdateAction {

    private KnowledgeBaseUpdateAction() {
    }

    public static ActionDefinition of(
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService,
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService) {

        VectorStore updateVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);

        return action(UPDATE)
            .title("Update Documents")
            .description(
                "Updates documents in the knowledge base by deleting existing ones matching the selected " +
                    "document or chunk and loading new ones.")
            .properties(
                bool(IS_MULTIPLE)
                    .label("Update Multiple")
                    .description("Whether to update multiple documents or chunks.")
                    .required(true),
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to update documents in.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseActionOptions(knowledgeBaseService))
                    .required(true),
                integer(KNOWLEDGE_BASE_DOCUMENT_ID)
                    .label("Document")
                    .description("The document to update in the knowledge base.")
                    .options(KnowledgeBaseOptionsUtils.documentActionOptions(knowledgeBaseDocumentService))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_ID)
                    .displayCondition(IS_MULTIPLE + "== false")
                    .required(false),
                integer(KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID)
                    .label("Document Chunk")
                    .description(
                        "The specific chunk to update. If not selected, all chunks of the selected document " +
                            "will be replaced.")
                    .options(KnowledgeBaseOptionsUtils.documentChunkActionOptions(knowledgeBaseDocumentChunkFacade))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_DOCUMENT_ID)
                    .displayCondition(IS_MULTIPLE + "== false")
                    .required(false),
                object(ADDITIONAL_METADATA)
                    .label("Additional Metadata")
                    .description("Additional metadata key-value pairs to add to the stored documents.")
                    .additionalProperties(
                        string(), integer(), number(), bool(), dateTime(), date(), time())
                    .displayCondition(IS_MULTIPLE + "== false")
                    .required(false),
                array(METADATA_FILTER)
                    .label("Metadata Filter")
                    .description(
                        "List of metadata key-value pairs to filter by. Entries within a group are AND-ed; groups are OR-ed.")
                    .items(
                        object()
                            .additionalProperties(
                                string(), integer(), number(), bool(), dateTime(), date(), time()))
                    .displayCondition(IS_MULTIPLE + "== true")
                    .required(false),
                string(CONTENT)
                    .label("Content")
                    .description(
                        "The text content to update the knowledge base with. If not provided, uses the configured " +
                            "document reader.")
                    .required(false))
            .perform((MultipleConnectionsPerformFunction) (
                inputParameters, componentConnections, extensions, context) -> perform(
                    inputParameters, componentConnections, extensions, context, updateVectorStore,
                    clusterElementDefinitionService));
    }

    private static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context, VectorStore updateVectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        String content = inputParameters.getString(CONTENT);

        DocumentReader documentReader = content != null
            ? () -> List.of(new Document(content))
            : getDocumentReader(extensions, componentConnections, context, clusterElementDefinitionService);

        List<DocumentTransformer> documentTransformers = content != null
            ? List.of()
            : getDocumentTransformers(extensions, componentConnections, clusterElementDefinitionService);

        updateVectorStore.update(inputParameters, ParametersFactory.create(Map.of()), null,
            documentReader, documentTransformers);

        return null;
    }

    private static DocumentReader getDocumentReader(
        Parameters extensions, Map<String, ComponentConnection> componentConnections, ActionContext context,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(DOCUMENT_READER);

        ComponentConnection componentConnection = componentConnections.get(KNOWLEDGE_BASE);

        DocumentReaderFunction documentReaderFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        return documentReaderFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(
                componentConnection == null ? Map.of() : componentConnection.getParameters()),
            ((ActionContextAware) context).toClusterElementContext(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName(), componentConnection));
    }

    private static List<DocumentTransformer> getDocumentTransformers(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return ClusterElementMap.of(extensions)
            .getClusterElements(DOCUMENT_TRANSFORMER)
            .stream()
            .map(clusterElement -> {
                ComponentConnection componentConnection = componentConnections.get(KNOWLEDGE_BASE);

                DocumentTransformerFunction documentTransformerFunction =
                    clusterElementDefinitionService.getClusterElement(
                        clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                        clusterElement.getClusterElementName());

                if (documentTransformerFunction instanceof DocumentSplitterFunction documentSplitterFunction) {
                    return documentSplitterFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(
                            componentConnection == null ? Map.of() : componentConnection.getParameters()));
                } else if (documentTransformerFunction instanceof DocumentEnricherFunction documentEnricherFunction) {
                    return documentEnricherFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(
                            componentConnection == null ? Map.of() : componentConnection.getParameters()),
                        ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
                } else {
                    throw new IllegalArgumentException(
                        "Unsupported transformer type: " + documentTransformerFunction.getClass());
                }
            })
            .toList();
    }

}
