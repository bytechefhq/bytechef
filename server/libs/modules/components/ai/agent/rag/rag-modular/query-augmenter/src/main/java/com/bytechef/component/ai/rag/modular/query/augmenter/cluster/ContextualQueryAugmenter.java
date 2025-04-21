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

/**
 * @author Ivica Cardic
 */
public class ContextualQueryAugmenter {

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
            .build();
    }
}
