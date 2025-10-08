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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.LOAD;
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
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractLoadAction {

    public final ActionDefinition actionDefinition;

    private final String componentName;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final VectorStore vectorStore;

    protected AbstractLoadAction(
        String componentName, VectorStore vectorStore, List<Property> properties,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.actionDefinition = action(LOAD)
            .title("Load Data")
            .description("Loads data into the vector store using LLM embeddings.")
            .properties(properties)
            .perform((MultipleConnectionsPerformFunction) this::perform);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentName = componentName;
        this.vectorStore = vectorStore;
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) {

        ComponentConnection vectorStoreComponentConnection = componentConnections.get(componentName);

        vectorStore.load(
            inputParameters,
            ParametersFactory.createParameters(vectorStoreComponentConnection.getParameters()),
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
            ParametersFactory.createParameters(clusterElement.getParameters()),
            ParametersFactory.createParameters(
                componentConnection == null ? Map.of() : componentConnection.getParameters()),
            ((ActionContextAware) context).createClusterElementContext(
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
                        ParametersFactory.createParameters(clusterElement.getParameters()),
                        ParametersFactory.createParameters(
                            componentConnection == null ? Map.of() : componentConnection.getParameters()));
                } else if (documentTransformerFunction instanceof DocumentEnricherFunction documentEnricherFunction) {
                    return documentEnricherFunction.apply(
                        ParametersFactory.createParameters(clusterElement.getParameters()),
                        ParametersFactory.createParameters(
                            componentConnection == null ? Map.of() : componentConnection.getParameters()),
                        ParametersFactory.createParameters(clusterElement.getExtensions()), componentConnections);
                } else {
                    throw new IllegalArgumentException(
                        "Unsupported transformer type: " + documentTransformerFunction.getClass());
                }
            })
            .toList();
    }
}
