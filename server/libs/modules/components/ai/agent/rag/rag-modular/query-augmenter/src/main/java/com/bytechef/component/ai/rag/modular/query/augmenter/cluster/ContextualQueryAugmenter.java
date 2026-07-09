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

package com.bytechef.component.ai.rag.modular.query.augmenter.cluster;

import static com.bytechef.platform.component.definition.ai.agent.rag.QueryAugmenterFunction.QUERY_AUGMENTER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.rag.QueryAugmenterFunction;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * @author Ivica Cardic
 */
public class ContextualQueryAugmenter {

    // Spring AI's default template forces "no prior knowledge, say you don't know", and allowEmptyContext=false emits
    // a canned refusal when nothing is retrieved. Both suppress an agent's bound tools and meta questions (e.g. "list
    // tools"). Treat the context as supplementary and allow empty context so the model can fall back to its tools.
    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate(
        """
            Context information is below.

            ---------------------
            {context}
            ---------------------

            Use the context above to help answer the query when it is relevant. You may also use the tools available
            to you and your own general knowledge. If the context does not contain the answer, rely on your tools and
            knowledge instead of refusing to answer.

            Query: {query}

            Answer:
            """);

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<QueryAugmenterFunction>clusterElement("contextualQueryAugmenter")
            .title("Contextual Query Augmenter")
            .description(
                """
                    Augments the user query with contextual data from the content of the provided
                    documents.
                    """)
            .type(QUERY_AUGMENTER)
            .object(() -> ContextualQueryAugmenter::apply);

    protected static org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter apply(
        Parameters inputParameters, Parameters connectionParameters) {

        return org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter.builder()
            .promptTemplate(PROMPT_TEMPLATE)
            .allowEmptyContext(true)
            .build();
    }
}
