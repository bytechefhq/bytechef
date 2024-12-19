/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.vectorstore.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableFileEntryProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Monika Kušter
 */
public class VectorStoreConstants {

    public static final String DATA_QUERY = "dataQuery";
    public static final String DOCUMENT = "document";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String EMBEDDING_API_KEY = "embeddingApiKey";
    public static final String JSON = "json";
    public static final String JSON_KEYS_TO_USE = "jsonKeysToUse";
    public static final String LOAD_DATA = "loadData";
    public static final String MD = "md";
    public static final String PDF = "pdf";
    public static final String TIKA = "tika";
    public static final String TXT = "txt";
    public static final String QUERY = "query";

    public static final ModifiableFileEntryProperty DOCUMENT_PROPERTY = fileEntry(DOCUMENT)
        .required(true);

    public static final ModifiableStringProperty DOCUMENT_TYPE_PROPERTY = string(DOCUMENT_TYPE)
        .label("Document Type")
        .description("The type of the document.")
        .options(
            option("JSON document", JSON),
            option("Markdown document", MD),
            option("PDF document", PDF),
            option("text document", TXT),
            option("Tika (DOCX, PPTX, HTML...)", TIKA))
        .required(true);

    public static final ModifiableStringProperty EMBEDDING_API_KEY_PROPERTY = string(EMBEDDING_API_KEY)
        .label("Open AI API Key")
        .description("The API key for the OpenAI API which is used to generate embeddings.")
        .required(true);

    public static final ModifiableArrayProperty JSON_KEYS_TO_USE_PROPERTY = array(JSON_KEYS_TO_USE)
        .label("JSON Keys to Use")
        .description(
            "Json keys on which extraction of content is based. If no keys are specified, it uses the entire " +
                "JSON object as content.")
        .displayCondition("%s == '%s'".formatted(DOCUMENT, JSON))
        .items(string())
        .required(false);

    private VectorStoreConstants() {
    }
}
