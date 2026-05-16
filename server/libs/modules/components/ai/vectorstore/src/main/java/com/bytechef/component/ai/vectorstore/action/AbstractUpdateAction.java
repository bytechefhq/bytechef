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

package com.bytechef.component.ai.vectorstore.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.UPDATE;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.util.VectorStoreUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;

/**
 * @author Monika Kušter
 */
public abstract class AbstractUpdateAction {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final String componentName;
    private final VectorStore vectorStore;

    protected AbstractUpdateAction(
        ClusterElementDefinitionService clusterElementDefinitionService, String componentName,
        VectorStore vectorStore) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentName = componentName;
        this.vectorStore = vectorStore;
    }

    public static ActionDefinition of(
        String componentName, List<Property> properties, VectorStore vectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        AbstractUpdateAction updateAction = new AbstractUpdateAction(
            clusterElementDefinitionService, componentName, vectorStore) {};

        return action(UPDATE)
            .title("Update Documents")
            .description(
                "Updates documents in the vector store by deleting existing ones matching the metadata filter " +
                    "and loading new ones using LLM embeddings.")
            .properties(
                Stream.of(properties.stream(), Stream.of(METADATA_PROPERTY))
                    .flatMap(stream -> stream)
                    .toList())
            .perform((MultipleConnectionsPerformFunction) updateAction::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) {

        ComponentConnection vectorStoreComponentConnection = componentConnections.get(componentName);

        vectorStore.update(
            inputParameters,
            ParametersFactory.create(
                vectorStoreComponentConnection == null ? Map.of() : vectorStoreComponentConnection.getParameters()),
            VectorStoreUtils.getEmbeddingModel(extensions, componentConnections, clusterElementDefinitionService),
            getDocumentReader(extensions, componentConnections, context),
            getDocumentTransformers(extensions, componentConnections));

        return null;
    }

    private DocumentReader getDocumentReader(
        Parameters extensions, Map<String, ComponentConnection> componentConnections, ActionContext context) {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(DOCUMENT_READER);

        ComponentConnection componentConnection = componentConnections.get(componentName);

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

    private List<DocumentTransformer> getDocumentTransformers(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        return ClusterElementMap.of(extensions)
            .getClusterElements(DOCUMENT_TRANSFORMER)
            .stream()
            .map(clusterElement -> {
                ComponentConnection componentConnection = componentConnections.get(componentName);

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
