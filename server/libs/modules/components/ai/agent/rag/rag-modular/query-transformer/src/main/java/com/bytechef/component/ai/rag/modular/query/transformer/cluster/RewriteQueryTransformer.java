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

package com.bytechef.component.ai.rag.modular.query.transformer.cluster;

import static com.bytechef.platform.component.definition.ai.agent.rag.QueryTransformerFunction.QUERY_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryTransformerFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

public class RewriteQueryTransformer extends AbstractQueryTransformer {

    public final ClusterElementDefinition<?> clusterElementDefinition =
        ComponentDsl.<QueryTransformerFunction>clusterElement("rewriteQueryTransformer")
            .title("Rewrite Query Transformer")
            .description(
                """
                    Uses a large language model to rewrite a user query to provide better results when
                    querying a target system, such as a vector store or a web search engine.
                    This transformer is useful when the user query is verbose, ambiguous, or contains
                    irrelevant information that may affect the quality of the search results.
                    """)
            .type(QUERY_TRANSFORMER)
            .object(() -> this::apply);

    public RewriteQueryTransformer(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
    }

    @Override
    protected QueryTransformer build(Parameters inputParameters, ChatModel chatModel) {
        return org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer.builder()
            .chatClientBuilder(ChatClient.builder(chatModel))
            .build();
    }
}
