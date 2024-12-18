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

package com.bytechef.component.pinecone.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pinecone.constant.PineconeConstants.API_KEY;
import static com.bytechef.component.pinecone.constant.PineconeConstants.DOCUMENT;
import static com.bytechef.component.pinecone.constant.PineconeConstants.DOCUMENT_TYPE;
import static com.bytechef.component.pinecone.constant.PineconeConstants.EMBEDDING_API_KEY;
import static com.bytechef.component.pinecone.constant.PineconeConstants.ENVIRONMENT;
import static com.bytechef.component.pinecone.constant.PineconeConstants.INDEX_NAME;
import static com.bytechef.component.pinecone.constant.PineconeConstants.JSON;
import static com.bytechef.component.pinecone.constant.PineconeConstants.JSON_KEYS_TO_USE;
import static com.bytechef.component.pinecone.constant.PineconeConstants.PROJECT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.PineconeVectorStore;
import org.springframework.ai.vectorstore.PineconeVectorStore.PineconeVectorStoreConfig;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Monika Kušter
 */
public class PineconeDataLoaderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("dataLoader")
        .title("Data Loader")
        .description("")
        .properties(
            string(DOCUMENT_TYPE)
                .label("Document Type")
                .description("The type of the document.")
                .options(
                    option("JSON document", JSON),
                    option("text document", "txt"),
                    option("PDF document", "pdf"),
                    option("Tika (DOCX, PPTX, HTML...)", "tika"))
                .required(true),
            array(JSON_KEYS_TO_USE)
                .label("JSON Keys to Use")
                .description(
                    "Json keys on which extraction of content is based. If no keys are specified, it uses the entire " +
                        "JSON object as content.")
                .displayCondition("%s == '%s".formatted(DOCUMENT, JSON))
                .items(string())
                .required(false),
            fileEntry(DOCUMENT)
                .required(true))
        .perform(PineconeDataLoaderAction::perform);

    private PineconeDataLoaderAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        PineconeVectorStore pineconeVectorStore = getPineconeVectorStore(connectionParameters);

        FileEntry fileEntry = inputParameters.getFileEntry(DOCUMENT);

        File file1 = actionContext.file(file -> file.toTempFile(fileEntry));
        FileSystemResource fileSystemResource = new FileSystemResource(file1);

        String documentType = inputParameters.getRequiredString(DOCUMENT_TYPE);

        switch (documentType) {
            case JSON -> {
                List<String> list = inputParameters.getList(JSON_KEYS_TO_USE, String.class);
                JsonReader jsonReader;
                if (list == null) {
                    jsonReader = new JsonReader(fileSystemResource);

                } else {
                    jsonReader = new JsonReader(fileSystemResource, list.toArray(new String[0]));
                }

                List<Document> documents = jsonReader.get();

                pineconeVectorStore.add(documents);
            }
            case "txt" -> {
                TextReader textReader = new TextReader(fileSystemResource);

                textReader.getCustomMetadata()
                    .put("filename", fileEntry.getName());

                List<Document> documents = textReader.read();
                pineconeVectorStore.add(documents);
            }
            case "pdf" -> {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(fileSystemResource,
                    PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                        .withPagesPerDocument(1)
                        .build());

                List<Document> documents = pdfReader.read();
                pineconeVectorStore.add(documents);
            }
            case "tika" -> {
                TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileSystemResource);

                List<Document> documents = tikaDocumentReader.read();

                pineconeVectorStore.add(documents);
            }
        }

        return null;
    }

    private static PineconeVectorStore getPineconeVectorStore(Parameters connectionParameters) {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)));

        PineconeVectorStoreConfig pineconeVectorStoreConfig = PineconeVectorStoreConfig.builder()
            .withApiKey(connectionParameters.getRequiredString(API_KEY))
            .withEnvironment(connectionParameters.getRequiredString(ENVIRONMENT))
            .withProjectId(connectionParameters.getRequiredString(PROJECT_ID))
            .withIndexName(connectionParameters.getRequiredString(INDEX_NAME))
            .build();

        return new PineconeVectorStore(pineconeVectorStoreConfig, openAiEmbeddingModel);
    }
}
