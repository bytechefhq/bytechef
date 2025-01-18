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

package com.bytechef.component.ai.vectorstore;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT_TYPE;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.EMBEDDING_API_KEY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.JSON_KEYS_TO_USE;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.KEYWORD_METADATA_ENRICHER;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.SUMMARY_METADATA_ENRICHER;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.TOKEN_TEXT_SPLITTER;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_KEYWORD_ENRICHER;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_SUMMARY_ENRICHER;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_TOKEN_TEXT_SPLITTER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Monika Kušter
 */
public interface VectorStore {

    enum DocumentType {
        JSON, MD, PDF, TXT, TIKA
    }

    org.springframework.ai.vectorstore.VectorStore createVectorStore(Parameters connectionParameters);

    default void loadData(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(connectionParameters);

        List<Document> documents = readDocuments(inputParameters, actionContext);

        documents =
            applyTransformers(documents, inputParameters, connectionParameters);

        vectorStore.add(documents);
    }

    private List<Document> readDocuments(Parameters inputParameters, ActionContext actionContext) {
        FileEntry fileEntry = inputParameters.getFileEntry(DOCUMENT);
        File file1 = actionContext.file(file -> file.toTempFile(fileEntry));
        FileSystemResource fileSystemResource = new FileSystemResource(file1);

        DocumentType documentType = inputParameters.getRequired(DOCUMENT_TYPE, DocumentType.class);
        return switch (documentType) {
            case JSON -> {
                List<String> keys = inputParameters.getList(JSON_KEYS_TO_USE, String.class);
                JsonReader jsonReader = (keys == null) ? new JsonReader(fileSystemResource)
                    : new JsonReader(fileSystemResource, keys.toArray(new String[0]));
                yield jsonReader.get();
            }
            case MD -> {
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", fileEntry.getName())
                    .build();
                yield new MarkdownDocumentReader(fileSystemResource, config).get();
            }
            case PDF -> {
                PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfTopTextLinesToDelete(0)
                        .build())
                    .withPagesPerDocument(1)
                    .build();
                yield new PagePdfDocumentReader(fileSystemResource, config).read();
            }
            case TIKA -> new TikaDocumentReader(fileSystemResource).read();
            case TXT -> {
                TextReader textReader = new TextReader(fileSystemResource);
                textReader.getCustomMetadata()
                    .put("filename", fileEntry.getName());
                yield textReader.read();
            }
            default -> throw new IllegalArgumentException("Unsupported document type: " + documentType);
        };
    }

    private List<Document> applyTransformers(
        List<Document> documents, Parameters inputParameters, Parameters connectionParameters) {

        if (inputParameters.getRequiredBoolean(USE_TOKEN_TEXT_SPLITTER)) {
            TextSplitter textSplitter = inputParameters.get(TOKEN_TEXT_SPLITTER, TextSplitter.class);

            TokenTextSplitter splitter = new TokenTextSplitter(
                textSplitter.defaultChunkSize(), textSplitter.minChunkSizeChars(), textSplitter.minChunkLengthToEmbed(),
                textSplitter.maxNumChunks(), textSplitter.keepSeparator());

            documents = splitter.apply(documents);
        }

        // TODO - use other ChatModel
        OpenAiChatModel openAiChatModel = new OpenAiChatModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)),
            OpenAiChatOptions.builder()
                .model("gpt-4")
                .build());

        if (inputParameters.getRequiredBoolean(USE_KEYWORD_ENRICHER)) {
            KeywordEnricher keywordEnricher = inputParameters.get(KEYWORD_METADATA_ENRICHER, KeywordEnricher.class);

            KeywordMetadataEnricher enricher =
                new KeywordMetadataEnricher(openAiChatModel, keywordEnricher.keywordCount());

            documents = enricher.apply(documents);
        }

        if (inputParameters.getRequiredBoolean(USE_SUMMARY_ENRICHER)) {
            SummaryEnricher summaryEnricher = inputParameters.get(SUMMARY_METADATA_ENRICHER, SummaryEnricher.class);

            List<SummaryMetadataEnricher.SummaryType> summaryTypes = summaryEnricher.summaryTypes()
                .stream()
                .map(SummaryMetadataEnricher.SummaryType::valueOf)
                .toList();

            SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(openAiChatModel, summaryTypes);

            documents = enricher.apply(documents);
        }

        return documents;
    }

    default List<Document> query(Parameters inputParameters, Parameters connectionParameters) {
        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(connectionParameters);

        return vectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
    }

    record KeywordEnricher(Integer keywordCount) {
    }

    @SuppressFBWarnings("EI")
    record SummaryEnricher(List<String> summaryTypes) {
    }

    record TextSplitter(
        int defaultChunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
        boolean keepSeparator) {
    }
}
