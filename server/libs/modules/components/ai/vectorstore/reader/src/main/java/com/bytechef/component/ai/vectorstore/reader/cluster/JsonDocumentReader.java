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

import static com.bytechef.component.ai.vectorstore.reader.util.DocumentReaderConstants.JSON_KEYS_TO_USE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction.DOCUMENT_READER;

import com.bytechef.component.ai.vectorstore.reader.util.DocumentReaderConstants;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentReaderFunction;
import java.util.List;
import org.springframework.ai.reader.JsonReader;

/**
 * @author Monika Kušter
 */
public class JsonDocumentReader extends AbstractDocumentReader {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentReaderFunction>clusterElement("json")
            .title("JSON Document Reader")
            .description("Reads JSON documents and converts them into a list of Document objects.")
            .type(DOCUMENT_READER)
            .properties(
                DocumentReaderConstants.DOCUMENT_PROPERTY,
                array(JSON_KEYS_TO_USE)
                    .label("JSON Keys to Use")
                    .description(
                        "Json keys on which extraction of content is based. If no keys are specified, it uses " +
                            "the entire JSON object as content.")
                    .items(string())
                    .required(false))
            .object(() -> JsonDocumentReader::apply);

    protected static JsonReader apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        FileResult result = getFile(inputParameters, context);

        List<String> keys = inputParameters.getList(JSON_KEYS_TO_USE, String.class);

        return (keys == null) ? new JsonReader(result.fileSystemResource())
            : new JsonReader(result.fileSystemResource(), keys.toArray(new String[0]));
    }
}
