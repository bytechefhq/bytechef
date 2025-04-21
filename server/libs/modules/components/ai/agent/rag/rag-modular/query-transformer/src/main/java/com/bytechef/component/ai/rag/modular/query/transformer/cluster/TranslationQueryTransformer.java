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

public class TranslationQueryTransformer extends AbstractQueryTransformer {

    private static final String TARGET_LANGUAGE = "targetLanguage";

    public final ClusterElementDefinition<?> clusterElementDefinition =
        ComponentDsl.<QueryTransformerFunction>clusterElement("translationQueryTransformer")
            .title("Translation Query Transformer")
            .description(
                """
                    Uses a large language model to translate a query to a target language that is supported
                    by the embedding model used to generate the document embeddings. If the query is
                    already in the target language, it is returned unchanged. If the language of the query
                    is unknown, it is also returned unchanged.
                    This transformer is useful when the embedding model is trained on a specific language
                    and the user query is in a different language.
                    """)
            .type(QUERY_TRANSFORMER)
            .properties(
                ComponentDsl.string(TARGET_LANGUAGE)
                    .label("Target Language")
                    .description(
                        """
                            The target language to which the query should be translated. The language
                            should be specified in ISO 639-1 format (e.g., "en" for English, "fr" for
                            French, etc.).
                            """)
                    .required(false))
            .object(() -> this::apply);

    public TranslationQueryTransformer(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
    }

    @Override
    protected QueryTransformer build(Parameters inputParameters, ChatModel chatModel) {
        return org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer.builder()
            .chatClientBuilder(ChatClient.builder(chatModel))
            .targetLanguage(inputParameters.getRequiredString(TARGET_LANGUAGE))
            .build();
    }
}
