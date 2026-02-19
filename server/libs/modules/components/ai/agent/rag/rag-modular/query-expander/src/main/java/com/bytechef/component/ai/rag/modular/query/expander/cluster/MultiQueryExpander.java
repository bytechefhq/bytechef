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

package com.bytechef.component.ai.rag.modular.query.expander.cluster;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.rag.QueryExpanderFunction.QUERY_EXPANDER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryExpanderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
public class MultiQueryExpander {

    private static final String INCLUDE_ORIGINAL = "includeOriginal";
    private static final String NUMBER_OF_QUERIES = "numberOfQueries";

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public MultiQueryExpander(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public final ClusterElementDefinition<?> clusterElementDefinition =
        ComponentDsl.<QueryExpanderFunction>clusterElement("multiQueryExpander")
            .title("Multi Query Expander")
            .description(
                """
                    Uses a large language model to expand a query into multiple semantically diverse
                    variations to capture different perspectives, useful for retrieving additional
                    contextual information and increasing the chances of finding relevant results.
                    """)
            .type(QUERY_EXPANDER)
            .properties(
                bool(INCLUDE_ORIGINAL)
                    .label("Include Original")
                    .description(
                        """
                            If true, the original query is included in the list of expanded queries.
                            If false, only the expanded queries are returned.
                            """)
                    .defaultValue(true),
                integer(NUMBER_OF_QUERIES)
                    .label("Number of Queries")
                    .description("The number of queries to generate. The default value is 3.")
                    .defaultValue(3))
            .object(() -> this::apply);

    protected org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        ChatModel chatModel = (ChatModel) modelFunction
            .apply(
                ParametersFactory.createParameters(clusterElement.getParameters()),
                ParametersFactory.createParameters(componentConnection.getParameters()), false);

        return new org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander(
            ChatClient.builder(chatModel), null, inputParameters.getBoolean(INCLUDE_ORIGINAL, true),
            inputParameters.getInteger(NUMBER_OF_QUERIES, 3));
    }
}
