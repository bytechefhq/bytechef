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
 * @author Monika Kušter
 */
public class TikaDocumentReader extends AbstractDocumentReader {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentReaderFunction>clusterElement("tika")
            .title("Tika Document Reader")
            .description(
                """
                    A document reader that leverages Apache Tika to extract text from a variety of document
                    formats, such as PDF, DOC/DOCX, PPT/PPTX, and HTML. For a comprehensive list of
                    supported formats, refer to: https://tika.apache.org/3.0.0/formats.html.
                    This reader directly provides the extracted text without any additional formatting. All
                    extracted texts are encapsulated within a Document object.
                    """)
            .type(DOCUMENT_READER)
            .properties(
                DOCUMENT_PROPERTY)
            .object(() -> TikaDocumentReader::apply);

    protected static org.springframework.ai.document.DocumentReader apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        FileResult result = getFile(inputParameters, context);

        return new org.springframework.ai.reader.tika.TikaDocumentReader(result.fileSystemResource());
    }
}
