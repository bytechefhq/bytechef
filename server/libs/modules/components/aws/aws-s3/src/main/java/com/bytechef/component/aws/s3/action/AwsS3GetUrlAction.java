
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

package com.bytechef.component.aws.s3.action;

import com.bytechef.component.aws.s3.util.AmazonS3Uri;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import static com.bytechef.component.aws.s3.constant.AwsS3Constant.GET_URL;
import static com.bytechef.component.aws.s3.constant.AwsS3Constant.URI;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwsS3GetUrlAction {

    public static final ActionDefinition ACTION_DEFINITION = action(GET_URL)
        .display(display("Get URL").description("Get the url of an AWS S3 object."))
        .properties(string(URI)
            .label("URI")
            .description("The AWS S3 uri.")
            .required(true))
        .outputSchema(string())
        .perform(AwsS3GetUrlAction::performGetUrl);

    public static String performGetUrl(Context context, Parameters parameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(parameters.getRequiredString(URI));

        String bucketName = amazonS3Uri.getBucket();
        String key = amazonS3Uri.getKey();

        S3ClientBuilder builder = S3Client.builder();

        try (S3Client s3Client = builder.build()) {
            return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build())
                .toString();
        }
    }
}
