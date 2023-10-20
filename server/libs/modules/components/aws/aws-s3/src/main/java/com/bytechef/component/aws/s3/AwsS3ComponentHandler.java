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

package com.bytechef.component.aws.s3;

import static com.bytechef.component.aws.s3.constants.AwsS3Constants.ACL;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.AWS_S3;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.BUCKET;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.FILENAME;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.FILE_ENTRY;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.GET_OBJECT;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.GET_URL;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.LIST_OBJECTS;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.PREFIX;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.PRESIGN_GET_OBJECT;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.PUT_OBJECT;
import static com.bytechef.component.aws.s3.constants.AwsS3Constants.URI;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author Ivica Cardic
 */
public class AwsS3ComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(AWS_S3)
            .display(display("AWS S3")
                    .description("AWS S3 is a simple object storage service provided by Amazon Web Services."))
            .actions(
                    action(GET_OBJECT)
                            .display(display("Get Object").description("Get the AWS S3 object."))
                            .properties(
                                    string(URI)
                                            .label("URI")
                                            .description("The AWS S3 uri.")
                                            .required(true),
                                    string(FILENAME)
                                            .label("Filename")
                                            .description("Filename to set for binary data.")
                                            .required(true)
                                            .defaultValue("file.xml"))
                            .output(fileEntry())
                            .perform(this::performGetObject),
                    action(GET_URL)
                            .display(display("Get URL").description("Get the url of an AWS S3 object."))
                            .properties(string(URI)
                                    .label("URI")
                                    .description("The AWS S3 uri.")
                                    .required(true))
                            .output(string())
                            .perform(this::performGetUrl),
                    action(LIST_OBJECTS)
                            .display(display("List Objects").description("Get the list AWS S3 objects."))
                            .properties(
                                    string(BUCKET)
                                            .label("Bucket")
                                            .description("The bucket to list AWS S3 objects from.")
                                            .required(true),
                                    string(PREFIX)
                                            .label("Prefix")
                                            .description("The prefix of an AWS S3 objects.")
                                            .required(true))
                            .output(array().items(object().properties(string("key"), string("suffix"), string("uri"))))
                            .perform(this::performListObjects),
                    action(PRESIGN_GET_OBJECT)
                            .display(display("Get Pre-signed Object")
                                    .description("Get the url of an pre-signed AWS S3 object."))
                            .properties(string(URI)
                                    .label("URI")
                                    .description("The AWS S3 uri.")
                                    .required(true))
                            .output(string())
                            .perform(this::performGetPresignedObject),
                    action(PUT_OBJECT)
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
                                    string(ACL).label("ACL").description("The canned ACL to apply to the object."))
                            .output(string())
                            .perform(this::performPutObject));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected FileEntry performGetObject(Context context, ExecutionParameters executionParameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(executionParameters.getRequiredString(URI));

        String bucketName = amazonS3Uri.getBucket();
        String key = amazonS3Uri.getKey();

        try (S3Client s3Client = S3Client.builder().build()) {
            return context.storeFileContent(
                    executionParameters.getRequiredString(FILENAME),
                    s3Client.getObject(
                            GetObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build(),
                            ResponseTransformer.toInputStream()));
        }
    }

    protected String performGetUrl(Context context, ExecutionParameters executionParameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(executionParameters.getRequiredString(URI));

        String bucketName = amazonS3Uri.getBucket();
        String key = amazonS3Uri.getKey();

        try (S3Client s3Client = S3Client.builder().build()) {
            return s3Client.utilities()
                    .getUrl(GetUrlRequest.builder().bucket(bucketName).key(key).build())
                    .toString();
        }
    }

    protected List<S3ObjectDescription> performListObjects(Context context, ExecutionParameters executionParameters) {
        try (S3Client s3Client = S3Client.builder().build()) {

            ListObjectsResponse response = s3Client.listObjects(ListObjectsRequest.builder()
                    .bucket(executionParameters.getRequiredString(BUCKET))
                    .prefix(executionParameters.getRequiredString(PREFIX))
                    .build());

            return response.contents().stream()
                    .map(o -> new S3ObjectDescription(executionParameters.getRequiredString(BUCKET), o))
                    .collect(Collectors.toList());
        }
    }

    protected String performGetPresignedObject(Context context, ExecutionParameters executionParameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(executionParameters.getRequiredString(URI));

        try (S3Presigner s3Presigner = S3Presigner.create()) {
            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(z -> z.signatureDuration(
                            Duration.parse("PT" + executionParameters.getRequiredString("signatureDuration")))
                    .getObjectRequest(por -> por.bucket(amazonS3Uri.getBucket()).key(amazonS3Uri.getKey())));

            return presignedGetObjectRequest.url().toString();
        }
    }

    protected Object performPutObject(Context context, ExecutionParameters executionParameters) {
        AmazonS3Uri amazonS3Uri = new AmazonS3Uri(executionParameters.getRequiredString(URI));

        String bucketName = amazonS3Uri.getBucket();
        String key = amazonS3Uri.getKey();

        FileEntry fileEntry = executionParameters.get(FILE_ENTRY, FileEntry.class);

        try (S3Client s3Client = S3Client.builder().build()) {
            Path tempFilePath = Files.createTempFile("", ".tmp");

            Files.copy(context.getFileStream(fileEntry), tempFilePath);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .acl(
                                    executionParameters.getString(ACL) != null
                                            ? ObjectCannedACL.fromValue(executionParameters.getString(ACL))
                                            : null)
                            .build(),
                    tempFilePath);

            return null;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    protected record S3ObjectDescription(String bucket, S3Object s3Object) {

        public String getKey() {
            return s3Object.key();
        }

        public String getSuffix() {
            return FilenameUtils.getName(getKey());
        }

        public String getUri() {
            return String.format("s3://%s/%s", bucket, getKey());
        }
    }
}
