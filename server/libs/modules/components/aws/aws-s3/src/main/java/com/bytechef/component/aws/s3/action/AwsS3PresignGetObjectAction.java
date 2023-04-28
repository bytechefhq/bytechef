
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

import static com.bytechef.component.aws.s3.constant.AwsS3Constant.PRESIGN_GET_OBJECT;
import static com.bytechef.component.aws.s3.constant.AwsS3Constant.URI;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwsS3PresignGetObjectAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PRESIGN_GET_OBJECT)
        .title("Get Pre-signed Object")
        .description("Get the url of an pre-signed AWS S3 object.")
        .properties(string(URI)
            .label("URI")
            .description("The AWS S3 uri.")
            .required(true))
        .outputSchema(string())
        .execute(AwsS3PresignGetObjectAction::executeGetPresignedObject);

    protected static String executeGetPresignedObject(Context context, InputParameters inputParameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(inputParameters.getRequiredString(URI));

        try (S3Presigner s3Presigner = S3Presigner.create()) {
            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(builder -> builder
                .signatureDuration(
                    Duration.parse("PT" + inputParameters.getRequiredString("signatureDuration")))
                .getObjectRequest(por -> por.bucket(amazonS3Uri.getBucket())
                    .key(amazonS3Uri.getKey())));

            URL url = presignedGetObjectRequest.url();

            return url.toString();
        }
    }
}
