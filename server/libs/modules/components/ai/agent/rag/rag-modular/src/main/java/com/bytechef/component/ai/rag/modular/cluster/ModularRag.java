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

package com.bytechef.component.ai.rag.modular.cluster;

import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;
import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentJoinerFunction.DOCUMENT_JOINER;
import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentRetrieverFunction.DOCUMENT_RETRIEVER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryAugmenterFunction.QUERY_AUGMENTER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryExpanderFunction.QUERY_EXPANDER;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryTransformerFunction.QUERY_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.RagFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentJoinerFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentRetrieverFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryAugmenterFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryExpanderFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryTransformerFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * @author Ivica Cardic
 */
public class ModularRag {

    public final ClusterElementDefinition<RagFunction> clusterElementDefinition =
        ComponentDsl.<RagFunction>clusterElement("rag")
            .title("Moduler RAG")
            .description("Moduler RAG.")
            .type(RAG)
            .object(() -> this::apply);

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public ModularRag(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    protected RetrievalAugmentationAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(getQueryTransformers(extensions, componentConnections))
            .queryExpander(getQueryExpander(extensions, componentConnections))
            .documentRetriever(getDocumentRetriever(extensions, componentConnections))
            .documentJoiner(getDocumentJoiner(extensions, componentConnections))
            .queryAugmenter(getQueryAugmenter(extensions, componentConnections))
            .build();
    }

    private DocumentJoiner getDocumentJoiner(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        return ClusterElementMap.of(extensions)
            .fetchClusterElement(DOCUMENT_JOINER)
            .map(clusterElement -> {
                DocumentJoinerFunction documentJoinerFunction = clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

                ComponentConnection componentConnection =
                    componentConnections.get(clusterElement.getWorkflowNodeName());

                try {
                    return documentJoinerFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(componentConnection.getParameters()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }

    private DocumentRetriever getDocumentRetriever(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        return ClusterElementMap.of(extensions)
            .fetchClusterElement(DOCUMENT_RETRIEVER)
            .map(clusterElement -> {
                DocumentRetrieverFunction documentRetrieverFunction =
                    clusterElementDefinitionService.getClusterElement(
                        clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                        clusterElement.getClusterElementName());

                ComponentConnection componentConnection =
                    componentConnections.get(clusterElement.getWorkflowNodeName());

                try {
                    return documentRetrieverFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(
                            componentConnection == null ? Map.of() : componentConnection.getParameters()),
                        ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }

    private QueryAugmenter getQueryAugmenter(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        return ClusterElementMap.of(extensions)
            .fetchClusterElement(QUERY_AUGMENTER)
            .map(clusterElement -> {
                QueryAugmenterFunction queryAugmenterFunction = clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

                ComponentConnection componentConnection =
                    componentConnections.get(clusterElement.getWorkflowNodeName());

                try {
                    return queryAugmenterFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(componentConnection.getParameters()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }

    private QueryExpander getQueryExpander(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        return ClusterElementMap.of(extensions)
            .fetchClusterElement(QUERY_EXPANDER)
            .map(clusterElement -> {
                QueryExpanderFunction queryExpanderFunction = clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

                ComponentConnection componentConnection =
                    componentConnections.get(clusterElement.getWorkflowNodeName());

                try {
                    return queryExpanderFunction.apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(componentConnection.getParameters()),
                        ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }

    private List<QueryTransformer> getQueryTransformers(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        List<QueryTransformer> queryTransformers = new ArrayList<>();

        List<ClusterElement> clusterElements = ClusterElementMap.of(extensions)
            .getClusterElements(QUERY_TRANSFORMER);

        for (ClusterElement clusterElement : clusterElements) {
            QueryTransformerFunction queryTransformerFunction =
                clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

            ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

            queryTransformers.add(
                queryTransformerFunction.apply(
                    ParametersFactory.create(clusterElement.getParameters()),
                    ParametersFactory.create(componentConnection.getParameters()),
                    ParametersFactory.create(clusterElement.getExtensions()), componentConnections));
        }

        return queryTransformers;
    }
}
