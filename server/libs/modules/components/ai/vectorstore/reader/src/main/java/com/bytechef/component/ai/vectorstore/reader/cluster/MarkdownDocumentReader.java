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

package com.bytechef.component.ai.vectorstore.reader.cluster;

import static com.bytechef.component.ai.vectorstore.reader.util.DocumentReaderConstants.DOCUMENT_PROPERTY;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;

/**
 * @author Monika Kušter
 */
public class MarkdownDocumentReader extends AbstractDocumentReader {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentReaderFunction>clusterElement("markdown")
            .title("Markdown Document Reader")
            .description(
                "Reads the given Markdown resource and groups headers, paragraphs, or text divided by horizontal " +
                    "lines into documents.")
            .type(DOCUMENT_READER)
            .properties(
                DOCUMENT_PROPERTY)
            .object(() -> MarkdownDocumentReader::apply);

    protected static org.springframework.ai.reader.markdown.MarkdownDocumentReader apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        FileResult result = getFile(inputParameters, context);

        FileEntry fileEntry = result.fileEntry();

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
            .withHorizontalRuleCreateDocument(true)
            .withIncludeCodeBlock(false)
            .withIncludeBlockquote(false)
            .withAdditionalMetadata("filename", fileEntry.getName())
            .build();

        return new org.springframework.ai.reader.markdown.MarkdownDocumentReader(result.fileSystemResource(), config);
    }
}
