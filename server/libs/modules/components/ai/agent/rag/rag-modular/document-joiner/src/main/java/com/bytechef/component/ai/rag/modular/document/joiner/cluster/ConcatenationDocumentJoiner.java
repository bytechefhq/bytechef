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

package com.bytechef.component.ai.rag.modular.document.joiner.cluster;

import static com.bytechef.platform.component.definition.ai.agent.rag.DocumentJoinerFunction.DOCUMENT_JOINER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentJoinerFunction;

public class ConcatenationDocumentJoiner {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentJoinerFunction>clusterElement("concatenationDocumentJoiner")
            .title("Concatenation Document Joiner")
            .description(
                """
                    Combines documents retrieved based on multiple queries and from multiple data sources
                    by concatenating them into a single collection of documents. In case of duplicate
                    documents, the first occurrence is kept. The score of each document is kept as is.
                    """)
            .type(DOCUMENT_JOINER)
            .object(() -> ConcatenationDocumentJoiner::apply);

    protected static org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner apply(
        Parameters inputParameters, Parameters connectionParameters) {

        return new org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner();
    }
}
