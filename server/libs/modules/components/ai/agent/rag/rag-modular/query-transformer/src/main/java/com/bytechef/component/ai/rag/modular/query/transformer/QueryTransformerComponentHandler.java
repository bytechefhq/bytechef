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

package com.bytechef.component.ai.rag.modular.query.transformer;

import static com.bytechef.component.ai.rag.modular.query.transformer.QueryTransformerComponentHandler.QUERY_TRANSFORMER;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.rag.modular.query.transformer.cluster.CompressionQueryTransformer;
import com.bytechef.component.ai.rag.modular.query.transformer.cluster.RewriteQueryTransformer;
import com.bytechef.component.ai.rag.modular.query.transformer.cluster.TranslationQueryTransformer;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.QueryTransformerComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(QUERY_TRANSFORMER + "_v1_ComponentHandler")
public class QueryTransformerComponentHandler implements ComponentHandler {

    public static final String QUERY_TRANSFORMER = "queryTransformer";

    private final QueryTransformerComponentDefinition componentDefinition;

    public QueryTransformerComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new QueryTransformerComponentDefinitionImpl(
            component(QUERY_TRANSFORMER)
                .title("Query Transformer")
                .description("Query Transformer.")
                .icon("path:assets/query-transformer.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new CompressionQueryTransformer(clusterElementDefinitionService).clusterElementDefinition,
                    new RewriteQueryTransformer(clusterElementDefinitionService).clusterElementDefinition,
                    new TranslationQueryTransformer(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class QueryTransformerComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements QueryTransformerComponentDefinition {

        public QueryTransformerComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
