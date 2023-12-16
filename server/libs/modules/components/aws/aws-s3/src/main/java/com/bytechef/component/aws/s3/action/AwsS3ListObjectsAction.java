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
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.LIST_OBJECTS;
import static com.bytechef.component.aws.s3.constant.AwsS3Constants.PREFIX;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.aws.s3.util.AwsS3Utils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ParameterMap;
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
 * @author Ivica Cardic
 */
public class AwsS3ListObjectsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_OBJECTS)
        .title("List Objects")
        .description("Get the list AWS S3 objects.")
        .properties(
            string(PREFIX)
                .label("Prefix")
                .description("The prefix of an AWS S3 objects.")
                .required(true))
        .outputSchema(array().items(object().properties(string("key"), string("suffix"), string("uri"))))
        .perform(AwsS3ListObjectsAction::perform);

    protected static List<S3ObjectDescription> perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, Context context) {

        try (S3Client s3Client = AwsS3Utils.buildS3Client(connectionParameters)) {
            ListObjectsResponse response = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(connectionParameters.getRequiredString(BUCKET_NAME))
                .prefix(inputParameters.getRequiredString(PREFIX))
                .build());

            return response.contents()
                .stream()
                .map(o -> new S3ObjectDescription(
                    connectionParameters.getRequiredString(BUCKET_NAME), o))
                .collect(Collectors.toList());
        }
    }

    private record S3ObjectDescription(String bucket, S3Object s3Object) {

        public String getKey() {
            return s3Object.key();
        }

        @SuppressFBWarnings("NP")
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
