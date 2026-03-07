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

package com.bytechef.component.ai.vectorstore.s3.constant;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * @author Marko Krišković
 */
public class S3Constants {

    private static final Logger logger = LoggerFactory.getLogger(S3Constants.class);

    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String BUCKET_NAME = "bucketName";
    public static final String KEY = "key";
    public static final String REGION = "region";
    public static final String S3_VECTOR_STORE = "s3VectorStore";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";

    public static final VectorStore VECTOR_STORE = new VectorStore() {

        @Override
        public org.springframework.ai.vectorstore.VectorStore createVectorStore(
            Parameters connectionParameters, EmbeddingModel embeddingModel) {

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

            loadFromS3(vectorStore, connectionParameters);

            return vectorStore;
        }

        @Override
        public void load(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel,
            DocumentReader documentReader, List<DocumentTransformer> documentTransformers) {

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

            loadFromS3(vectorStore, connectionParameters);

            List<Document> documents = documentReader.read();

            for (DocumentTransformer transformer : documentTransformers) {
                documents = transformer.transform(documents);
            }

            vectorStore.add(documents);

            saveToS3(vectorStore, connectionParameters);
        }

        @Override
        public List<Document> search(
            Parameters inputParameters, Parameters connectionParameters, EmbeddingModel embeddingModel) {

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

            loadFromS3(vectorStore, connectionParameters);

            return vectorStore.similaritySearch(inputParameters.getRequiredString(QUERY));
        }

        private void loadFromS3(SimpleVectorStore vectorStore, Parameters connectionParameters) {
            String bucketName = connectionParameters.getRequiredString(BUCKET_NAME);
            String key = connectionParameters.getRequiredString(KEY);

            try (S3Client s3Client = createS3Client(connectionParameters)) {
                File tempFile = File.createTempFile("s3-vector-store", ".json");

                try {
                    s3Client.getObject(
                        GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                        tempFile.toPath());

                    vectorStore.load(tempFile);
                } catch (NoSuchKeyException e) {
                    logger.debug("No existing vector store data found in S3 at {}/{}, starting with empty store",
                        bucketName, key);
                } finally {
                    Files.delete(tempFile.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load vector store from S3", e);
            }
        }

        private void saveToS3(SimpleVectorStore vectorStore, Parameters connectionParameters) {
            try (S3Client s3Client = createS3Client(connectionParameters)) {
                File tempFile = File.createTempFile("s3-vector-store", ".json");

                try {
                    vectorStore.save(tempFile);

                    s3Client.putObject(
                        PutObjectRequest.builder()
                            .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                            .key(connectionParameters.getRequiredString(KEY))
                            .build(),
                        tempFile.toPath());
                } finally {
                    Files.delete(tempFile.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save vector store to S3", e);
            }
        }

        private S3Client createS3Client(Parameters connectionParameters) {
            return S3Client.builder()
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            connectionParameters.getRequiredString(ACCESS_KEY_ID),
                            connectionParameters.getRequiredString(SECRET_ACCESS_KEY))))
                .region(Region.of(connectionParameters.getRequiredString(REGION)))
                .build();
        }
    };

    private S3Constants() {
    }
}
