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

import static com.bytechef.component.ai.vectorstore.s3.vectorstore.S3TextPreservingVectorStore.CONTENT_METADATA_KEY;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.s3.vectorstore.S3TextPreservingVectorStore;
import com.bytechef.component.definition.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.s3.S3VectorStore;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.ConflictException;
import software.amazon.awssdk.services.s3vectors.model.CreateIndexRequest;
import software.amazon.awssdk.services.s3vectors.model.DataType;
import software.amazon.awssdk.services.s3vectors.model.DistanceMetric;
import software.amazon.awssdk.services.s3vectors.model.GetIndexRequest;
import software.amazon.awssdk.services.s3vectors.model.MetadataConfiguration;
import software.amazon.awssdk.services.s3vectors.model.NotFoundException;

/**
 * @author Marko Krišković
 */
public class S3Constants {

    private static final Logger log = LoggerFactory.getLogger(S3Constants.class);

    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String DISTANCE_METRIC = "distanceMetric";
    public static final String INDEX_NAME = "indexName";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String REGION = "region";
    public static final String S3_VECTOR_STORE = "s3VectorStore";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String VECTOR_BUCKET_NAME = "vectorBucketName";

    public static final VectorStore VECTOR_STORE = (inputParameters, connectionParameters, embeddingModel) -> {
        String indexName = connectionParameters.getRequiredString(INDEX_NAME);
        String vectorBucketName = connectionParameters.getRequiredString(VECTOR_BUCKET_NAME);

        S3VectorsClient s3VectorsClient = createS3VectorsClient(connectionParameters);

        if (connectionParameters.getBoolean(INITIALIZE_SCHEMA, true)) {
            createIndexIfMissing(s3VectorsClient, vectorBucketName, indexName, connectionParameters, embeddingModel);
        }

        S3VectorStore s3VectorStore = new S3VectorStore.Builder(s3VectorsClient, embeddingModel)
            .vectorBucketName(vectorBucketName)
            .indexName(indexName)
            .build();

        return new S3TextPreservingVectorStore(s3VectorStore);
    };

    private static S3VectorsClient createS3VectorsClient(Parameters connectionParameters) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
            connectionParameters.getRequiredString(ACCESS_KEY_ID),
            connectionParameters.getRequiredString(SECRET_ACCESS_KEY));

        return S3VectorsClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .build();
    }

    /**
     * An S3 Vectors index cannot be created lazily on first write: its dimension, distance metric and the set of
     * non-filterable metadata keys are all fixed at creation time. The document text is stored as non-filterable
     * metadata because filterable metadata is capped at 2 KB per vector, which a typical chunk exceeds.
     */
    private static void createIndexIfMissing(
        S3VectorsClient s3VectorsClient, String vectorBucketName, String indexName, Parameters connectionParameters,
        EmbeddingModel embeddingModel) {

        try {
            s3VectorsClient.getIndex(
                GetIndexRequest.builder()
                    .vectorBucketName(vectorBucketName)
                    .indexName(indexName)
                    .build());

            return;
        } catch (NotFoundException notFoundException) {
            log.debug(
                "No index {} found in vector bucket {}, creating it", indexName, vectorBucketName, notFoundException);
        }

        DistanceMetric distanceMetric = DistanceMetric.fromValue(
            connectionParameters.getString(DISTANCE_METRIC, DistanceMetric.COSINE.toString()));

        try {
            s3VectorsClient.createIndex(
                CreateIndexRequest.builder()
                    .vectorBucketName(vectorBucketName)
                    .indexName(indexName)
                    .dataType(DataType.FLOAT32)
                    .dimension(embeddingModel.dimensions())
                    .distanceMetric(distanceMetric)
                    .metadataConfiguration(
                        MetadataConfiguration.builder()
                            .nonFilterableMetadataKeys(CONTENT_METADATA_KEY)
                            .build())
                    .build());
        } catch (ConflictException conflictException) {
            log.debug(
                "Index {} in vector bucket {} was created concurrently", indexName, vectorBucketName,
                conflictException);
        }
    }

    private S3Constants() {
    }
}
