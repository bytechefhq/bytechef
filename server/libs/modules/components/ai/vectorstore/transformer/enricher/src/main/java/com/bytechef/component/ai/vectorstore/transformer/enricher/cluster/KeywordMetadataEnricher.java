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

package com.bytechef.component.ai.vectorstore.transformer.enricher.cluster;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentEnricherFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.springframework.ai.document.DocumentTransformer;

/**
 * @author Monika Kušter
 */
public class KeywordMetadataEnricher extends AbstractMetadataEnricher {

    private static final String KEYWORD_METADATA_ENRICHER = "keywordMetadataEnricher";

    public final ClusterElementDefinition<?> clusterElementDefinition =
        ComponentDsl.<DocumentEnricherFunction>clusterElement("keywordMetadataEnricher")
            .title("Keyword Metadata Enricher")
            .description("Keyword extractor that uses generative to extract 'excerpt_keywords' metadata field.")
            .properties(
                object(KEYWORD_METADATA_ENRICHER)
                    .label("Keyword Metadata Enricher")
                    .description("Extract keywords from document content and add them as metadata.")
                    .properties(
                        integer("keywordCount")
                            .label("Keyword Count")
                            .description("The number of keywords to extract for each document.")
                            .required(true))
                    .required(true))
            .type(DOCUMENT_TRANSFORMER)
            .object(() -> this::apply);

    public KeywordMetadataEnricher(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
    }

    protected DocumentTransformer apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        KeywordEnricher keywordEnricher = inputParameters.get(KEYWORD_METADATA_ENRICHER, KeywordEnricher.class);

        return new org.springframework.ai.model.transformer.KeywordMetadataEnricher(
            getModel(extensions, componentConnections), keywordEnricher.keywordCount());
    }

    record KeywordEnricher(Integer keywordCount) {
    }
}
