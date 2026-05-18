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

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_PROPERTY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.UPDATE;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
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
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService) {

        VectorStore deleteVectorStore = createVectorStore(vectorStore);
        VectorStore loadVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);

        return action(UPDATE)
            .title("Update Documents")
            .description(
                "Updates documents in the knowledge base by deleting existing ones matching the metadata filter " +
                    "and loading new ones.")
            .properties(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to update documents in.")
                    .options(getKnowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                METADATA_PROPERTY)
            .perform((MultipleConnectionsPerformFunction) (
                inputParameters, componentConnections, extensions, context) -> perform(
                    inputParameters, componentConnections, extensions, context, deleteVectorStore, loadVectorStore,
                    clusterElementDefinitionService));
    }

    private static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context, VectorStore deleteVectorStore, VectorStore loadVectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        ComponentConnection vectorStoreComponentConnection = componentConnections.get(KNOWLEDGE_BASE);

        deleteVectorStore.delete(inputParameters, ParametersFactory.create(Map.of()), null);

        loadVectorStore.load(
            inputParameters,
            ParametersFactory.create(
                vectorStoreComponentConnection == null ? Map.of() : vectorStoreComponentConnection.getParameters()),
            null,
            getDocumentReader(extensions, componentConnections, context, clusterElementDefinitionService),
            getDocumentTransformers(extensions, componentConnections, clusterElementDefinitionService));

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

    private static ActionDefinition.OptionsFunction<Long> getKnowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                String knowledgeBaseName = knowledgeBase.getName();

                String knowledgeBaseNameLowerCase = knowledgeBaseName.toLowerCase(Locale.ROOT);

                if (searchText == null || knowledgeBaseNameLowerCase.contains(searchText.toLowerCase(Locale.ROOT))) {
                    Long knowledgeBaseId = knowledgeBase.getId();

                    options.add(option(knowledgeBaseName, knowledgeBaseId.longValue()));
                }
            }

            return options;
        };
    }
}
