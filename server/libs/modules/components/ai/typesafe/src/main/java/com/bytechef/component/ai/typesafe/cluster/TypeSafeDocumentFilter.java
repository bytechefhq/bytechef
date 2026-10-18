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
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CONTRADICTION_THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.EVIDENCE_THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INJECTION_THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.RELEVANCE_THRESHOLD;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentPostProcessorFunction.DOCUMENT_POST_PROCESSOR;

import com.bytechef.component.ai.typesafe.util.TypeSafeClientUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentPostProcessorFunction;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.rag.JevDocumentFilter;
import org.springaicommunity.typesafe.rag.JevDocumentFilter.Policy;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeDocumentFilter {

    private static final Policy DEFAULT_POLICY = Policy.defaults();

    public static final ClusterElementDefinition<DocumentPostProcessorFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentPostProcessorFunction>clusterElement("jevDocumentFilter")
            .title("Jev Document Filter")
            .description(
                "Screens every retrieved passage against the question with Jev before it reaches the prompt. " +
                    "Passages that carry a prompt injection, are not relevant or hold no answering evidence are " +
                    "dropped; passages that contradict the question's premise are kept and tagged CONFLICTING. " +
                    "Each passage costs one Jev call, so place the filter before a reranker.")
            .type(DOCUMENT_POST_PROCESSOR)
            .properties(
                CLUSTER_ELEMENT_MODEL_PROPERTY,
                number(INJECTION_THRESHOLD)
                    .label("Injection Threshold")
                    .description("A passage whose prompt-injection value is above this is dropped.")
                    .defaultValue(DEFAULT_POLICY.injection())
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                number(CONTRADICTION_THRESHOLD)
                    .label("Contradiction Threshold")
                    .description(
                        "A passage whose contradicts-the-premise value is above this is kept and tagged CONFLICTING.")
                    .defaultValue(DEFAULT_POLICY.contradiction())
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                number(RELEVANCE_THRESHOLD)
                    .label("Relevance Threshold")
                    .description("A passage whose relevance value is below this is dropped.")
                    .defaultValue(DEFAULT_POLICY.relevance())
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                number(EVIDENCE_THRESHOLD)
                    .label("Evidence Threshold")
                    .description("A passage is kept only when its answer-evidence value is above this.")
                    .defaultValue(DEFAULT_POLICY.evidence())
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                CONCURRENCY_PROPERTY)
            .object(() -> TypeSafeDocumentFilter::apply);

    private TypeSafeDocumentFilter() {
    }

    static JevDocumentFilter apply(Parameters inputParameters, Parameters connectionParameters) {
        return create(inputParameters, TypeSafeClientUtils.createTypeSafeClient(inputParameters, connectionParameters));
    }

    static JevDocumentFilter create(Parameters inputParameters, TypeSafeClient typeSafeClient) {
        return JevDocumentFilter.builder(typeSafeClient)
            .policy(
                new Policy(
                    inputParameters.getDouble(INJECTION_THRESHOLD, DEFAULT_POLICY.injection()),
                    inputParameters.getDouble(CONTRADICTION_THRESHOLD, DEFAULT_POLICY.contradiction()),
                    inputParameters.getDouble(RELEVANCE_THRESHOLD, DEFAULT_POLICY.relevance()),
                    inputParameters.getDouble(EVIDENCE_THRESHOLD, DEFAULT_POLICY.evidence())))
            .batchOptions(TypeSafeClientUtils.getBatchOptions(inputParameters))
            .build();
    }
}
