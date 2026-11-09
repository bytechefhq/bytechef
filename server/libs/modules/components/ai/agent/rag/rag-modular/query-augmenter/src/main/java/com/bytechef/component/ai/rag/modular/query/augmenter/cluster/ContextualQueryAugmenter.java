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

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
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

    static final String ALLOW_EMPTY_CONTEXT = "allowEmptyContext";
    static final String EMPTY_CONTEXT_PROMPT_TEMPLATE = "emptyContextPromptTemplate";
    static final String PROMPT_TEMPLATE = "promptTemplate";

    private static final String DEFAULT_PROMPT_TEMPLATE = """
        Context information is below.

        ---------------------
        {context}
        ---------------------

        Use the context above to help answer the query when it is relevant. You may also use the tools available
        to you and your own general knowledge. If the context does not contain the answer, rely on your tools and
        knowledge instead of refusing to answer.

        Query: {query}

        Answer:
        """;

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<QueryAugmenterFunction>clusterElement("contextualQueryAugmenter")
            .title("Contextual Query Augmenter")
            .description(
                """
                    Augments the user query with contextual data from the content of the provided
                    documents.
                    """)
            .type(QUERY_AUGMENTER)
            .properties(
                bool(ALLOW_EMPTY_CONTEXT)
                    .label("Allow Empty Context")
                    .description(
                        "When on, the query goes to the model even if no documents were retrieved, so an agent can " +
                            "still answer with its tools. Turn it off for document Q&A: an empty retrieval then " +
                            "sends the Empty Context Prompt Template instead of the query.")
                    .defaultValue(true),
                string(PROMPT_TEMPLATE)
                    .label("Prompt Template")
                    .description(
                        "The prompt that combines the retrieved documents with the query. It must contain the " +
                            "{context} and {query} placeholders. When empty and Allow Empty Context is on, the context "
                            +
                            "is treated as supplementary and the model may also use its tools and general " +
                            "knowledge. When empty and Allow Empty Context is off, the model answers only from the " +
                            "context and says it does not know otherwise.")
                    .controlType(TEXT_AREA)
                    .advancedOption(true),
                string(EMPTY_CONTEXT_PROMPT_TEMPLATE)
                    .label("Empty Context Prompt Template")
                    .description(
                        "The prompt sent instead of the query when no documents were retrieved and Allow Empty " +
                            "Context is off. When empty, the model is told the query is outside its knowledge " +
                            "base and to say so politely.")
                    .controlType(TEXT_AREA)
                    .displayCondition("%s == false".formatted(ALLOW_EMPTY_CONTEXT))
                    .advancedOption(true))
            .object(() -> ContextualQueryAugmenter::apply);

    protected static org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter apply(
        Parameters inputParameters, Parameters connectionParameters) {

        boolean allowEmptyContext = inputParameters.getBoolean(ALLOW_EMPTY_CONTEXT, true);

        org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter.Builder builder =
            org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter.builder()
                .allowEmptyContext(allowEmptyContext);

        String promptTemplate = inputParameters.getString(PROMPT_TEMPLATE);

        if (promptTemplate != null && !promptTemplate.isBlank()) {
            builder.promptTemplate(new PromptTemplate(promptTemplate));
        } else if (allowEmptyContext) {
            builder.promptTemplate(new PromptTemplate(DEFAULT_PROMPT_TEMPLATE));
        }

        String emptyContextPromptTemplate = inputParameters.getString(EMPTY_CONTEXT_PROMPT_TEMPLATE);

        if (emptyContextPromptTemplate != null && !emptyContextPromptTemplate.isBlank()) {
            builder.emptyContextPromptTemplate(new PromptTemplate(emptyContextPromptTemplate));
        }

        return builder.build();
    }
}
