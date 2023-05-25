
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
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.MapValueUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.ACL;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.BUCKET_NAME;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.FILE_ENTRY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.KEY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PUT_OBJECT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;

import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwsS3PutObjectAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PUT_OBJECT)
        .title("Put Object")
        .description("Store an object to AWS S3.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the file that needs to be written to AWS S3.")
                .required(true),
            string(KEY)
                .label("Key")
                .description("The object key.")
                .required(true),
            string(ACL)
                .label("ACL")
                .description("The canned ACL to apply to the object.")
                .options(
                    option("authenticated-read", "authenticated-read"),
                    option("aws-exec-read", "aws-exec-read"),
                    option("bucket-owner-read", "bucket-owner-read"),
                    option("bucket-owner-full-control", "bucket-owner-full-control"),
                    option("private", "private"),
                    option("public-read", "public-read"),
                    option("public-read-write", "public-read-write")))
        .outputSchema(string())
        .perform(AwsS3PutObjectAction::perform);

    protected static Object perform(Map<String, ?> inputParameters, Context context) {
        Connection connection = context.getConnection();
        FileEntry fileEntry = MapValueUtils.getRequired(inputParameters, FILE_ENTRY, FileEntry.class);

        try (S3Client s3Client = AwsS3Utils.buildS3Client(connection)) {
            Map<String, Object> connectionInputParameters = connection.getParameters();
            Path tempFilePath = Files.createTempFile("", ".tmp");

            Files.copy(context.getFileStream(fileEntry), tempFilePath);

            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(MapValueUtils.getRequiredString(connectionInputParameters, BUCKET_NAME))
                    .key(MapValueUtils.getRequiredString(inputParameters, KEY))
                    .acl(MapValueUtils.getString(inputParameters, ACL) != null
                        ? ObjectCannedACL.fromValue(MapValueUtils.getString(inputParameters, ACL))
                        : null)
                    .build(),
                tempFilePath);

            return null;
        } catch (IOException ioe) {
            throw new ComponentExecutionException(ioe.getMessage(), ioe);
        }
    }
}
