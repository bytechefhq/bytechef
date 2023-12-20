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

package com.bytechef.component.aws.s3.util;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.ACCESS_KEY_ID;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.REGION;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.SECRET_ACCESS_KEY;

import com.bytechef.hermes.component.definition.Parameters;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Ivica Cardic
 */
public class AwsS3Utils {

    public static S3Client buildS3Client(Parameters connectionParameters) {
        S3ClientBuilder builder = S3Client.builder()
            .credentialsProvider(getCredentialsProvider(connectionParameters))
            .region(Region.of(connectionParameters.getRequiredString(REGION)));

        return builder.build();
    }

    public static S3Presigner buildS3Presigner(Parameters connectionParameters) {
        S3Presigner.Builder builder = S3Presigner.builder()
            .credentialsProvider(getCredentialsProvider(connectionParameters))
            .region(Region.of(connectionParameters.getRequiredString(REGION)));

        return builder.build();
    }

    private static StaticCredentialsProvider getCredentialsProvider(Parameters connectionParameters) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
            connectionParameters.getRequiredString(ACCESS_KEY_ID),
            connectionParameters.getRequiredString(SECRET_ACCESS_KEY)));
    }
}
