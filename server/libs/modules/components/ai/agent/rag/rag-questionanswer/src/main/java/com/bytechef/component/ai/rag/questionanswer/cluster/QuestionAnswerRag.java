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

package com.bytechef.component.ai.rag.questionanswer.cluster;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;
import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;
import static org.springframework.ai.vectorstore.SearchRequest.DEFAULT_TOP_K;
import static org.springframework.ai.vectorstore.SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.RagFunction;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;

/**
 * @author Ivica Cardic
 */
public class QuestionAnswerRag {

    private static final String FILTER_EXPRESSION = "filterExpression";
    private static final String SIMILARITY_THRESHOLD = "similarityThreshold";
    private static final String TOP_K = "topK";

    public final ClusterElementDefinition<RagFunction> clusterElementDefinition =
        ComponentDsl.<RagFunction>clusterElement("rag")
            .title("Question Answer RAG")
            .description(
                "Context for the question is retrieved from a Vector Store and added to the prompt's user text.")
            .type(RAG)
            .properties(
                string(FILTER_EXPRESSION)
                    .label("Filter Expression")
                    .description("Filter expression to filter the results.")
                    .required(false),
                number(SIMILARITY_THRESHOLD)
                    .label("Similarity Threshold")
                    .description(
                        """
                            Similarity threshold score to filter the search response by. Only documents
                            with similarity score equal or greater than the 'threshold' will be returned.
                            Note that this is a post-processing step performed on the client not the server
                            A threshold value of 0.0 means any similarity is accepted or disable the
                            similarity threshold filtering. A threshold value of 1.0 means an exact match
                            is required.
                            """)
                    .minValue(0)
                    .maxValue(1)
                    .defaultValue(SIMILARITY_THRESHOLD_ACCEPT_ALL)
                    .required(false),
                integer(TOP_K)
                    .label("Top K")
                    .description("The top 'k' similar results to return.")
                    .minValue(0)
                    .defaultValue(DEFAULT_TOP_K)
                    .required(false))
            .object(() -> this::apply);

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public QuestionAnswerRag(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    protected QuestionAnswerAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(VECTOR_STORE);

        VectorStoreFunction vectorStoreFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());
        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return QuestionAnswerAdvisor
            .builder(
                vectorStoreFunction.apply(
                    ParametersFactory.create(clusterElement.getParameters()),
                    ParametersFactory.create(componentConnection.getParameters()),
                    ParametersFactory.create(clusterElement.getExtensions()), componentConnections))
            .searchRequest(
                SearchRequest.builder()
                    .filterExpression(inputParameters.getString(FILTER_EXPRESSION))
                    .similarityThreshold(
                        inputParameters.getDouble(SIMILARITY_THRESHOLD, SIMILARITY_THRESHOLD_ACCEPT_ALL))
                    .topK(inputParameters.getInteger(TOP_K, DEFAULT_TOP_K))
                    .build())
            .build();
    }
}
