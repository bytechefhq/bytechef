
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
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bytechef.component.aws.s3.constant.AwsS3Constant.ACL;
import static com.bytechef.component.aws.s3.constant.AwsS3Constant.FILE_ENTRY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constant.PUT_OBJECT;
import static com.bytechef.component.aws.s3.constant.AwsS3Constant.URI;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwsS3PutObjectAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PUT_OBJECT)
        .display(display("Put Object").description("Store an object to AWS S3."))
        .properties(
            string(URI)
                .label("URI")
                .description("The AWS S3 uri.")
                .required(true),
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the file that needs to be written to AWS S3.")
                .required(true),
            string(ACL).label("ACL")
                .description("The canned ACL to apply to the object."))
        .outputSchema(string())
        .perform(AwsS3PutObjectAction::performPutObject);

    public static Object performPutObject(Context context, Parameters parameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(parameters.getRequiredString(URI));

        String bucketName = amazonS3Uri.getBucket();
        String key = amazonS3Uri.getKey();

        S3ClientBuilder builder = S3Client.builder();
        Context.FileEntry fileEntry = parameters.get(FILE_ENTRY, Context.FileEntry.class);

        try (S3Client s3Client = builder.build()) {
            Path tempFilePath = Files.createTempFile("", ".tmp");

            Files.copy(context.getFileStream(fileEntry), tempFilePath);

            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(
                        parameters.getString(ACL) != null
                            ? ObjectCannedACL.fromValue(parameters.getString(ACL))
                            : null)
                    .build(),
                tempFilePath);

            return null;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
