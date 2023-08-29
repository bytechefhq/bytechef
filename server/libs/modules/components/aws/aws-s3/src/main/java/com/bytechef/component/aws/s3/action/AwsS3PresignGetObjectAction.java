
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

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.util.MapUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.BUCKET_NAME;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.KEY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PRESIGN_GET_OBJECT;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.SIGNATURE_DURATION;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwsS3PresignGetObjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(PRESIGN_GET_OBJECT)
        .title("Get Pre-signed Object")
        .description("Get the url of an pre-signed AWS S3 object.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The object key.")
                .required(true),
            string(SIGNATURE_DURATION)
                .label("Signature Duration")
                .placeholder("15M, 10H, PT-6H3M, etc.")
                .required(true))
        .outputSchema(string())
        .perform(AwsS3PresignGetObjectAction::perform);

    protected static String perform(Map<String, ?> inputParameters, Context context) {
        Connection connection = context.getConnection();

        Map<String, Object> connectionParameters = connection.getParameters();

        try (S3Presigner s3Presigner = AwsS3Utils.buildS3Presigner(connection)) {
            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(
                presignedObjectBuilder -> presignedObjectBuilder
                    .signatureDuration(
                        Duration.parse(
                            "PT" + MapUtils.getRequiredString(connectionParameters, SIGNATURE_DURATION)))
                    .getObjectRequest(
                        requestBuilder -> requestBuilder
                            .bucket(MapUtils.getRequiredString(connectionParameters, BUCKET_NAME))
                            .key(MapUtils.getRequiredString(inputParameters, KEY))));

            URL url = presignedGetObjectRequest.url();

            return url.toString();
        }
    }
}
