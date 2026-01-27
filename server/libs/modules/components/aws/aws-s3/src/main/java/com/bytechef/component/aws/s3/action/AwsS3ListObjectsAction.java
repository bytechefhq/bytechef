/*
 * Copyright 2025 ByteChef
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
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PREFIX;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * AWS S3 list objects action for workflow automation. Lists objects in an S3 bucket with a given prefix.
 *
 * @author Ivica Cardic
 */
public class AwsS3ListObjectsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listObjects")
        .title("List Objects")
        .description("Get the list AWS S3 objects. Every object needs to have read permission in order to be seen.")
        .properties(
            string(PREFIX)
                .label("Prefix")
                .description("The prefix of an AWS S3 objects.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string("key"),
                                string("suffix"),
                                string("uri")))))
        .perform(AwsS3ListObjectsAction::perform);

    /**
     * Security Note: PATH_TRAVERSAL_IN - Path traversal is intentional. The AWS S3 component allows workflow creators
     * to list S3 objects. The prefix is provided by the workflow creator, not end users. Access is controlled by AWS
     * IAM credentials configured in the connection.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    protected static List<S3ObjectDescription> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        try (S3Client s3Client = AwsS3Utils.buildS3Client(connectionParameters)) {
            ListObjectsResponse response = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                .prefix(inputParameters.getRequiredString(PREFIX))
                .build());

            return response.contents()
                .stream()
                .map(o -> new S3ObjectDescription(connectionParameters.getRequiredString(BUCKET_NAME), o))
                .collect(Collectors.toList());
        }
    }

    protected record S3ObjectDescription(String bucket, S3Object s3Object) {

        public String getKey() {
            return s3Object.key();
        }

        @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "Paths.get() is only used to parse S3 key and extract filename; no file I/O is performed")
        public String getSuffix() {
            Path path = Paths.get(getKey());

            Path fileName = Validate.notNull(path.getFileName(), "fileName");

            return fileName.toString();
        }

        public String getUri() {
            return String.format("s3://%s/%s", bucket, getKey());
        }
    }
}
