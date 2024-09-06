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

package com.bytechef.component.aws.s3.action;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.BUCKET_NAME;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.KEY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PRESIGN_GET_OBJECT;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.SIGNATURE_DURATION;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.net.URL;
import java.time.Duration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author Ivica Cardic
 */
public class AwsS3PresignGetObjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(PRESIGN_GET_OBJECT)
        .title("Get Pre-signed Object")
        .description("You can share an object with a pre-signed URL for up to 12 hours or until your session expires.")
        .properties(
            string(KEY)
                .label("Key")
                .description("Key is most likely the name of the file.")
                .placeholder("file.txt")
                .required(true),
            string(SIGNATURE_DURATION)
                .label("Signature Duration")
                .description("Time interval until the pre-signed URL expires")
                .placeholder("15M, 10H, PT-6H3M, etc.")
                .required(true))
        .output(outputSchema(string()))
        .perform(AwsS3PresignGetObjectAction::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (S3Presigner s3Presigner = AwsS3Utils.buildS3Presigner(connectionParameters)) {
            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(
                presignedObjectBuilder -> presignedObjectBuilder
                    .signatureDuration(
                        Duration.parse(
                            "PT" + inputParameters.getRequiredString(SIGNATURE_DURATION)))
                    .getObjectRequest(
                        requestBuilder -> requestBuilder
                            .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                            .key(inputParameters.getRequiredString(KEY))
                            .build()));

            URL url = presignedGetObjectRequest.url();

            return url.toString();
        }
    }
}
