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
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction;

/**
 * @author Ivica Cardic
 */
public class HtmlJsoupDocumentReader extends AbstractDocumentReader {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentReaderFunction>clusterElement("htmlJsoup")
            .title("HTML (Jsoup) Document Reader")
            .description(
                """
                    A document reader that uses Jsoup to parse and extract text content from HTML documents.
                    Jsoup provides robust HTML parsing with support for CSS selectors and DOM traversal.
                    Suitable for processing HTML files and web content.
                    """)
            .type(DOCUMENT_READER)
            .properties(
                DOCUMENT_PROPERTY)
            .object(() -> HtmlJsoupDocumentReader::apply);

    protected static org.springframework.ai.document.DocumentReader apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        FileResult result = getFile(inputParameters, context);

        return new org.springframework.ai.reader.jsoup.JsoupDocumentReader(result.fileSystemResource());
    }
}
