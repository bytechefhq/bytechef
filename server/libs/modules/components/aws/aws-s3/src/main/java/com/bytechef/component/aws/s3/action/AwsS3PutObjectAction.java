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

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.ACL;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.BUCKET_NAME;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.FILE_ENTRY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.KEY;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PUT_OBJECT;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * @author Ivica Cardic
 */
public class AwsS3PutObjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(PUT_OBJECT)
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
                .description("Key is most likely the name of the file.")
                .placeholder("file.txt")
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

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);

        try (S3Client s3Client = AwsS3Utils.buildS3Client(connectionParameters)) {
            File directory = new File("/tmp/bytechef/AWS");
            if(!directory.exists()) directory.mkdirs();

            Path tempFilePath = Files.createTempFile(directory.toPath(), "", ".tmp");

            Files.copy((InputStream) context.file(file -> file.getStream(fileEntry)), tempFilePath,
                StandardCopyOption.REPLACE_EXISTING);

            PutObjectResponse putObjectResponse = s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                    .key(inputParameters.getRequiredString(KEY))
                    .acl(inputParameters.getString(ACL) != null
                        ? ObjectCannedACL.fromValue(inputParameters.getString(ACL))
                        : null)
                    .build(),
                tempFilePath);

            return putObjectResponse.versionId();
        }
    }
}
