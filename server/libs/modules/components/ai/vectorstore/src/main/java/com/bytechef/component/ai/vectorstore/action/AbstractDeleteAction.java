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
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.DELETE;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.util.VectorStoreUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Monika Kušter
 */
public abstract class AbstractDeleteAction {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final String componentName;
    private final VectorStore vectorStore;

    protected AbstractDeleteAction(
        String componentName, VectorStore vectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentName = componentName;
        this.vectorStore = vectorStore;
    }

    public static ActionDefinition of(
        String componentName, VectorStore vectorStore, List<Property> properties,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        AbstractDeleteAction deleteAction = new AbstractDeleteAction(
            componentName, vectorStore, clusterElementDefinitionService) {};

        return action(DELETE)
            .title("Delete Documents")
            .description("Delete documents from the vector store by metadata")
            .properties(
                Stream
                    .of(
                        Stream.of(METADATA_PROPERTY),
                        properties.stream())
                    .flatMap(stream -> stream)
                    .toList())
            .perform((MultipleConnectionsPerformFunction) deleteAction::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) {

        ComponentConnection vectorStoreComponentConnection = componentConnections.get(componentName);
        EmbeddingModel embeddingModel = VectorStoreUtils.getEmbeddingModel(
            extensions, componentConnections, clusterElementDefinitionService);

        vectorStore.delete(
            inputParameters, ParametersFactory.create(vectorStoreComponentConnection.getParameters()),
            embeddingModel);

        return null;
    }
}
