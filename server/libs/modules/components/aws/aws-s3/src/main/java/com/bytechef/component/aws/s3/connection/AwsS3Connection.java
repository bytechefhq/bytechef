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

package com.bytechef.component.aws.s3.connection;

import static com.bytechef.component.aws.s3.constant.AwsS3Constants.BUCKET_NAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.aws.s3.constant.AwsS3Constants;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;

public class AwsS3Connection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .properties(
                    string(AwsS3Constants.ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(AwsS3Constants.SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .required(true),
                    string(AwsS3Constants.REGION)
                        .options(
                            option("US East (N. Virginia) [us-east-1]", "us-east-1"),
                            option("US East (Ohio) [us-east-2]", "us-east-2"),
                            option("US West (N. California) [us-west-1]", "us-west-1"),
                            option("US West (Oregon) [us-west-2]", "us-west-2"),
                            option("Canada (Central) [ca-central-1]", "ca-central-1"),
                            option("Asia Pacific (Hong Kong) [ap-east-1]", "ap-east-1"),
                            option("Asia Pacific (Mumbai) [ap-south-1]", "ap-south-1"),
                            option("Asia Pacific (Hyderabad) [ap-south-2]", "ap-south-2"),
                            option("Asia Pacific (Osaka-Local) [ap-northeast-3]", "ap-northeast-3"),
                            option("Asia Pacific (Seoul) [ap-northeast-2]", "ap-northeast-2"),
                            option("Asia Pacific (Singapore) [ap-southeast-1]", "ap-southeast-1"),
                            option("Asia Pacific (Sydney) [ap-southeast-2]", "ap-southeast-2"),
                            option("Asia Pacific (Jakarta) [ap-southeast-3]", "ap-southeast-3"),
                            option("Asia Pacific (Melbourne) [ap-southeast-4]", "ap-southeast-4"),
                            option("Asia Pacific (Tokyo) [ap-northeast-1]", "ap-northeast-1"),
                            option("Middle East (Bahrain) [me-south-1]", "me-south-1"),
                            option("Middle East (UAE) [me-central-1]", "me-central-1"),
                            option("Europe (Frankfurt) [eu-central-1]", "eu-central-1"),
                            option("Europe (Zurich) [eu-central-2]", "eu-central-2"),
                            option("Europe (Ireland) [eu-west-1]", "eu-west-1"),
                            option("Europe (London) [eu-west-2]", "eu-west-2"),
                            option("Europe (Milan) [eu-south-1]", "eu-south-1"),
                            option("Europe (Spain) [eu-south-2]", "eu-south-2"),
                            option("Europe (Paris) [eu-west-3]", "eu-west-3"),
                            option("Europe (Stockholm) [eu-north-1]", "eu-north-1"),
                            option("Africa (Cape Town) [af-south-1]", "af-south-1"),
                            option("South America (São Paulo) [sa-east-1]", "sa-east-1"),
                            option("China (Beijing) [cn-north-1]", "cn-north-1"),
                            option("China (Ningxia) [cn-northwest-1]", "cn-northwest-1"))
                        .required(true)
                        .defaultValue("us-east-1"),
                    string(BUCKET_NAME)
                        .label("Bucket")
                        .required(true)));
}
