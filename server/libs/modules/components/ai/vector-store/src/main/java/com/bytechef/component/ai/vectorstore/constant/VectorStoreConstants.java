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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableFileEntryProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.List;
import org.springframework.ai.transformer.SummaryMetadataEnricher;

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
    public static final String KEYWORD_METADATA_ENRICHER = "keywordMetadataEnricher";
    public static final String LOAD_DATA = "loadData";
    public static final String MD = "md";
    public static final String PDF = "pdf";
    public static final String QUERY = "query";
    public static final String SUMMARY_METADATA_ENRICHER = "summaryMetadataEnricher";
    public static final String TIKA = "tika";
    public static final String TOKEN_TEXT_SPLITTER = "tokenTextSplitter";
    public static final String TXT = "txt";
    public static final String USE_KEYWORD_ENRICHER = "useKeywordEnricher";
    public static final String USE_SUMMARY_ENRICHER = "useSummaryEnricher";
    public static final String USE_TOKEN_TEXT_SPLITTER = "useTokenTextSplitter";

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
        .displayCondition("%s == '%s'".formatted(DOCUMENT_TYPE, JSON))
        .items(string())
        .required(false);

    public static final ModifiableObjectProperty KEYWORD_METADATA_ENRICHER_PROPERTY = object(KEYWORD_METADATA_ENRICHER)
        .label("Keyword Metadata Enricher")
        .description("Extract keywords from document content and add them as metadata.")
        .properties(
            integer("keywordCount")
                .label("Keyword Count")
                .description("The number of keywords to extract for each document.")
                .required(true))
        .displayCondition("%s == true".formatted(USE_KEYWORD_ENRICHER))
        .required(true);

    public static final ModifiableObjectProperty SUMMARY_METADATA_ENRICHER_PROPERTY = object(SUMMARY_METADATA_ENRICHER)
        .label("Summary Metadata Enricher")
        .description("Summarize the document content and add the summaries as metadata.")
        .properties(
            array("summaryTypes")
                .label("Summary Types")
                .description("A list of summary types indicating which summaries to generate.")
                .options(
                    List.of(option("Previous", SummaryMetadataEnricher.SummaryType.PREVIOUS.name()),
                        option("Current", SummaryMetadataEnricher.SummaryType.CURRENT.name()),
                        option("Next", SummaryMetadataEnricher.SummaryType.NEXT.name())))
                .items(string())
                .required(true))
        .displayCondition("%s == true".formatted(USE_SUMMARY_ENRICHER))
        .required(true);

    public static final ModifiableObjectProperty TOKEN_TEXT_SPLITTER_PROPERTY = object(TOKEN_TEXT_SPLITTER)
        .label("Token Text Splitter")
        .description("Splits text into chunks based on token count, using the CL100K_BASE encoding.")
        .properties(
            integer("defaultChunkSize")
                .label("Default Chunk Size")
                .description("The target size of each text chunk in tokens.")
                .defaultValue(800)
                .required(true),
            integer("minChunkSizeChars")
                .label("Minimum Chunk Size Characters")
                .description("The minimum size of each text chunk in characters.")
                .defaultValue(350)
                .required(true),
            integer("minChunkLengthToEmbed")
                .label("Minimum Chunk Length to Embed")
                .description("The minimum length of a chunk to be included.")
                .defaultValue(5)
                .required(true),
            integer("maxNumChunks")
                .label("Maximum Number of Chunks")
                .description("The maximum number of chunks to generate from a text.")
                .defaultValue(10000)
                .required(true),
            bool("keepSeparator")
                .label("Keep Separator")
                .description("Whether to keep separators (like newlines) in the chunks.")
                .defaultValue(true)
                .required(true))
        .displayCondition("%s == true".formatted(USE_TOKEN_TEXT_SPLITTER))
        .required(true);

    public static final ModifiableBooleanProperty USE_KEYWORD_ENRICHER_PROPERTY = bool(USE_KEYWORD_ENRICHER)
        .label("Use Keyword Metadata Enricher")
        .description("Whether to use the keyword metadata enricher.")
        .defaultValue(false)
        .required(true);

    public static final ModifiableBooleanProperty USE_SUMMARY_ENRICHER_PROPERTy = bool(USE_SUMMARY_ENRICHER)
        .label("Use Summary Metadata Enricher")
        .description("Whether to use the summary enricher.")
        .defaultValue(false)
        .required(true);

    public static final ModifiableBooleanProperty USE_TOKEN_TEXT_SPLITTER_PROPERTY = bool(USE_TOKEN_TEXT_SPLITTER)
        .label("Use Token Text Splitter")
        .description("Whether to use the token text splitter.")
        .defaultValue(false)
        .required(true);

    private VectorStoreConstants() {
    }
}
