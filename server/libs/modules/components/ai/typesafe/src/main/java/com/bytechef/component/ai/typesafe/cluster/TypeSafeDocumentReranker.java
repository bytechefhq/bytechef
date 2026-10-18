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

package com.bytechef.component.ai.typesafe.cluster;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CLUSTER_ELEMENT_MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CONCURRENCY_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MINIMUM_SCORE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TOP_K;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentPostProcessorFunction.DOCUMENT_POST_PROCESSOR;

import com.bytechef.component.ai.typesafe.util.TypeSafeClientUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentPostProcessorFunction;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.rag.JevDocumentReranker;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeDocumentReranker {

    private static final int DEFAULT_TOP_K = 5;

    public static final ClusterElementDefinition<DocumentPostProcessorFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentPostProcessorFunction>clusterElement("jevDocumentReranker")
            .title("Jev Reranker")
            .description(
                "Orders the retrieved passages by how likely each is to answer the question, asked of Jev once per " +
                    "passage, and keeps the best ones. The score asks whether a passage states the answer, not " +
                    "whether it covers the same subject.")
            .type(DOCUMENT_POST_PROCESSOR)
            .properties(
                CLUSTER_ELEMENT_MODEL_PROPERTY,
                integer(TOP_K)
                    .label("Top K")
                    .description("The number of passages to keep.")
                    .defaultValue(DEFAULT_TOP_K)
                    .minValue(1),
                number(MINIMUM_SCORE)
                    .label("Minimum Score")
                    .description("Passages scoring below this are dropped even when fewer than Top K remain.")
                    .defaultValue(0)
                    .minValue(0)
                    .maxValue(1),
                string(INSTRUCTIONS)
                    .label("Instructions")
                    .description(
                        "Replaces the built-in yes/no question each passage is scored with. The state holds the " +
                            "`query` and the `passage`, so refer to them by those names.")
                    .controlType(TEXT_AREA)
                    .advancedOption(true),
                CONCURRENCY_PROPERTY)
            .object(() -> TypeSafeDocumentReranker::apply);

    private TypeSafeDocumentReranker() {
    }

    static JevDocumentReranker apply(Parameters inputParameters, Parameters connectionParameters) {
        return create(inputParameters, TypeSafeClientUtils.createTypeSafeClient(inputParameters, connectionParameters));
    }

    static JevDocumentReranker create(Parameters inputParameters, TypeSafeClient typeSafeClient) {
        JevDocumentReranker.Builder builder = JevDocumentReranker.builder(typeSafeClient)
            .topK(inputParameters.getInteger(TOP_K, DEFAULT_TOP_K))
            .minimumScore(inputParameters.getDouble(MINIMUM_SCORE, 0.0))
            .batchOptions(TypeSafeClientUtils.getBatchOptions(inputParameters));

        String instructions = inputParameters.getString(INSTRUCTIONS);

        if (instructions != null && !instructions.isBlank()) {
            builder.question(Noul.of(instructions));
        }

        return builder.build();
    }
}
