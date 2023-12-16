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
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.FILENAME;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.GET_OBJECT;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.KEY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * @author Ivica Cardic
 */
public class AwsS3GetObjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_OBJECT)
        .title("Get Object")
        .description("Get the AWS S3 object.")
        .properties(
            string(FILENAME)
                .label("Filename")
                .description("Filename to set for binary data.")
                .required(true)
                .defaultValue("file.txt"),
            string(KEY)
                .label("Key")
                .description("The object key.")
                .required(true))
        .outputSchema(fileEntry())
        .perform(AwsS3GetObjectAction::perform);

    protected static FileEntry perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        try (S3Client s3Client = AwsS3Utils.buildS3Client(connectionParameters)) {
            return context.file(file -> file.storeContent(inputParameters.getRequiredString(FILENAME),
                s3Client.getObject(
                    GetObjectRequest.builder()
                        .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                        .key(inputParameters.getRequiredString(KEY))
                        .build(),
                    ResponseTransformer.toInputStream())));
        }
    }
}
