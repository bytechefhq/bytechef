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
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.JSON;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.JSON_KEYS_TO_USE;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.PDF;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.TIKA;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.TXT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Monika Kušter
 */
public interface VectorStore {

    org.springframework.ai.vectorstore.VectorStore createVectorStore(Parameters connectionParameters);

    default void loadData(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(connectionParameters);

        FileEntry fileEntry = inputParameters.getFileEntry(DOCUMENT);

        File file1 = actionContext.file(file -> file.toTempFile(fileEntry));
        FileSystemResource fileSystemResource = new FileSystemResource(file1);

        String documentType = inputParameters.getRequiredString(DOCUMENT_TYPE);

        switch (documentType) {
            case JSON -> {
                JsonReader jsonReader;
                List<String> list = inputParameters.getList(JSON_KEYS_TO_USE, String.class);

                if (list == null) {
                    jsonReader = new JsonReader(fileSystemResource);
                } else {
                    jsonReader = new JsonReader(fileSystemResource, list.toArray(new String[0]));
                }

                List<Document> documents = jsonReader.get();

                vectorStore.add(documents);
            }
            case PDF -> {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    fileSystemResource,
                    PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                        .withPagesPerDocument(1)
                        .build());

                vectorStore.add(pdfReader.read());
            }
            case TIKA -> {
                TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileSystemResource);

                List<Document> documents = tikaDocumentReader.read();

                vectorStore.add(documents);
            }
            case TXT -> {
                TextReader textReader = new TextReader(fileSystemResource);

                Map<String, Object> customMetadata = textReader.getCustomMetadata();

                customMetadata.put("filename", fileEntry.getName());

                vectorStore.add(textReader.read());
            }
            default -> throw new IllegalArgumentException("Unsupported document type: " + documentType);
        }
    }

    default List<Document> query(Parameters inputParameters, Parameters connectionParameters) {
        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(connectionParameters);

        return vectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
    }
}
