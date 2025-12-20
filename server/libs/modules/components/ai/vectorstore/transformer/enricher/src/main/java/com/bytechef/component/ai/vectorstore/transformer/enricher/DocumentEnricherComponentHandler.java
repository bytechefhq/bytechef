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

package com.bytechef.component.ai.vectorstore.transformer.enricher;

import static com.bytechef.component.ai.vectorstore.transformer.enricher.DocumentEnricherComponentHandler.DOCUMENT_ENRICHER;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.transformer.enricher.cluster.KeywordMetadataEnricher;
import com.bytechef.component.ai.vectorstore.transformer.enricher.cluster.SummaryMetadataEnricher;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.DocumentEnricherComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(DOCUMENT_ENRICHER + "_v1_ComponentHandler")
public class DocumentEnricherComponentHandler implements ComponentHandler {

    public static final String DOCUMENT_ENRICHER = "documentEnricher";

    private final ComponentDefinition componentDefinition;

    public DocumentEnricherComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new DocumentEnricherDefinitionImpl(
            component(DOCUMENT_ENRICHER)
                .title("Document Enricher")
                .description("Document Enricher.")
                .icon("path:assets/document-enricher.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new KeywordMetadataEnricher(clusterElementDefinitionService).clusterElementDefinition,
                    new SummaryMetadataEnricher(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class DocumentEnricherDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements DocumentEnricherComponentDefinition {

        public DocumentEnricherDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
