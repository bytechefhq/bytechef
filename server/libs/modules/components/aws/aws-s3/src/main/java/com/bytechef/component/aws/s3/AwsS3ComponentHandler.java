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

package com.bytechef.component.aws.s3;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.AWS_S3;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.aws.s3.action.AwsS3GetObjectAction;
import com.bytechef.component.aws.s3.action.AwsS3GetUrlAction;
import com.bytechef.component.aws.s3.action.AwsS3ListObjectsAction;
import com.bytechef.component.aws.s3.action.AwsS3PresignGetObjectAction;
import com.bytechef.component.aws.s3.action.AwsS3PutObjectAction;
import com.bytechef.component.aws.s3.connection.AwsS3Connection;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class AwsS3ComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(AWS_S3)
        .title("AWS S3")
        .description("AWS S3 is a simple object storage service provided by Amazon Web Services.")
        .icon("path:assets/aws-s3.svg")
        .actions(
            AwsS3GetObjectAction.ACTION_DEFINITION, AwsS3GetUrlAction.ACTION_DEFINITION,
            AwsS3ListObjectsAction.ACTION_DEFINITION,
            AwsS3PresignGetObjectAction.ACTION_DEFINITION, AwsS3PutObjectAction.ACTION_DEFINITION)
        .connection(AwsS3Connection.CONNECTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
