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

package com.bytechef.component.ai.vectorstore;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Monika Kušter
 */
@FunctionalInterface
public interface VectorStore {

    org.springframework.ai.vectorstore.VectorStore createVectorStore(
        Parameters connectionParameters, EmbeddingModel embeddingModel);

    default void load(
        Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
        DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(
            connectionParameters, embeddingModel);

        List<Document> documents = documentReader.read();

        for (DocumentTransformer documentTransformer : documentTransformers) {
            documents = documentTransformer.transform(documents);
        }

        vectorStore.add(documents);
    }

    default List<Document> search(
        Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

        org.springframework.ai.vectorstore.VectorStore vectorStore = createVectorStore(
            connectionParameters, embeddingModel);

        return vectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
    }
}
