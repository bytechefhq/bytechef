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

import com.bytechef.component.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.s3.S3VectorStore;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;

/**
 * @author Ivica Cardic
 */
public class S3Constants {

    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String INDEX_NAME = "indexName";
    public static final String REGION = "region";
    public static final String S3 = "s3VectorStore";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String VECTOR_BUCKET_NAME = "vectorBucketName";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
            connectionParameters.getRequiredString(ACCESS_KEY_ID),
            connectionParameters.getRequiredString(SECRET_ACCESS_KEY));

        S3VectorsClient s3VectorsClient = S3VectorsClient.builder()
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();

        return new S3VectorStore.Builder(s3VectorsClient, embeddingModel)
            .indexName(connectionParameters.getRequiredString(INDEX_NAME))
            .vectorBucketName(connectionParameters.getRequiredString(VECTOR_BUCKET_NAME))
            .build();
    };

    private S3Constants() {
    }
}
