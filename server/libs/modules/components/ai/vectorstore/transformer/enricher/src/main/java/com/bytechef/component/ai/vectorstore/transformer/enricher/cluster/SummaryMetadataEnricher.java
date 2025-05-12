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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentEnricherFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;

/**
 * @author Monika Kušter
 */
public class SummaryMetadataEnricher extends AbstractMetadataEnricher {

    private static final String SUMMARY_METADATA_ENRICHER = "summaryMetadataEnricher";

    public final ClusterElementDefinition<?> clusterElementDefinition =
        ComponentDsl.<DocumentEnricherFunction>clusterElement("summaryMetadataEnricher")
            .title("Summary Metadata Enricher")
            .description(
                "Title extractor with adjacent sharing that uses generative to extract " +
                    "'section_summary', 'prev_section_summary', 'next_section_summary' metadata fields.")
            .properties(
                object(SUMMARY_METADATA_ENRICHER)
                    .label("Summary Metadata Enricher")
                    .description("Summarize the document content and add the summaries as metadata.")
                    .properties(
                        array("summaryTypes")
                            .label("Summary Types")
                            .description("A list of summary types indicating which summaries to generate.")
                            .options(
                                List.of(
                                    option("Previous", SummaryType.PREVIOUS.name()),
                                    option("Current", SummaryType.CURRENT.name()),
                                    option("Next", SummaryType.NEXT.name())))
                            .items(string())
                            .required(true))
                    .required(true))
            .type(DOCUMENT_TRANSFORMER)
            .object(() -> this::apply);

    public SummaryMetadataEnricher(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
    }

    protected DocumentTransformer apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        SummaryEnricher summaryEnricher = inputParameters.get(SUMMARY_METADATA_ENRICHER, SummaryEnricher.class);

        List<SummaryType> summaryTypes = summaryEnricher.summaryTypes()
            .stream()
            .map(SummaryType::valueOf)
            .toList();

        return new org.springframework.ai.model.transformer.SummaryMetadataEnricher(
            getModel(extensions, componentConnections), summaryTypes);
    }

    @SuppressFBWarnings("EI")
    record SummaryEnricher(List<String> summaryTypes) {
    }
}
