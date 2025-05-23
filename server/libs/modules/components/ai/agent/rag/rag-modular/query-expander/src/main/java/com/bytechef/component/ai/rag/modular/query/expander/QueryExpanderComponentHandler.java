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

package com.bytechef.component.ai.rag.modular.query.expander;

import static com.bytechef.component.ai.rag.modular.query.expander.QueryExpanderComponentHandler.QUERY_EXPANDER;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.rag.modular.query.expander.cluster.MultiQueryExpander;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.QueryExpanderComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(QUERY_EXPANDER + "_v1_ComponentHandler")
public class QueryExpanderComponentHandler implements ComponentHandler {

    public static final String QUERY_EXPANDER = "queryExpander";

    private final QueryExpanderComponentDefinition componentDefinition;

    public QueryExpanderComponentHandler(@Lazy ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new QueryExpanderComponentDefinitionImpl(
            component(QUERY_EXPANDER)
                .title("Query Expander")
                .description("Query Expander.")
                .icon("path:assets/query-expander.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new MultiQueryExpander(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class QueryExpanderComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements QueryExpanderComponentDefinition {

        public QueryExpanderComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
