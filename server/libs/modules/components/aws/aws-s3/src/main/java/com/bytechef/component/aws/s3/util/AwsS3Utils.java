
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.util.MapUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.ACCESS_KEY_ID;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.REGION;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.SECRET_ACCESS_KEY;

/**
 * @author Ivica Cardic
 */
public class AwsS3Utils {

    public static S3Client buildS3Client(Context.Connection connection) {
        S3ClientBuilder builder = S3Client.builder()
            .credentialsProvider(getCredentialsProvider(connection))
            .region(Region.of(MapUtils.getRequiredString(connection.getParameters(), REGION)));

        return builder.build();
    }

    public static S3Presigner buildS3Presigner(Context.Connection connection) {
        S3Presigner.Builder builder = S3Presigner.builder()
            .credentialsProvider(getCredentialsProvider(connection))
            .region(Region.of(MapUtils.getRequiredString(connection.getParameters(), REGION)));

        return builder.build();
    }

    private static StaticCredentialsProvider getCredentialsProvider(Context.Connection connection) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
            MapUtils.getRequiredString(connection.getParameters(), ACCESS_KEY_ID),
            MapUtils.getRequiredString(connection.getParameters(), SECRET_ACCESS_KEY)));
    }
}
