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

package com.bytechef.component.amazon.bedrock.connection;

import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.ACCESS_KEY_ID;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class AmazonBedrockConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .required(true),
                    string(REGION)
                        .options(
                            option("US East (N. Virginia) [us-east-1]", "us-east-1"),
                            option("US West (Oregon) [us-west-2]", "us-west-2"),
                            option("Asia Pacific (Mumbai) [ap-south-1]", "ap-south-1"),
                            option("Asia Pacific (Singapore) [ap-southeast-1]", "ap-southeast-1"),
                            option("Asia Pacific (Sydney) [ap-southeast-2]", "ap-southeast-2"),
                            option("Asia Pacific (Tokyo) [ap-northeast-1]", "ap-northeast-1"),
                            option("Canada (Central) [ca-central-1]", "ca-central-1"),
                            option("Europe (Frankfurt) [eu-central-1]", "eu-central-1"),
                            option("Europe (Ireland) [eu-west-1]", "eu-west-1"),
                            option("Europe (London) [eu-west-2]", "eu-west-2"),
                            option("Europe (Paris) [eu-west-3]", "eu-west-3"),
                            option("South America (São Paulo) [sa-east-1]", "sa-east-1"))
                        .required(true)
                        .defaultValue("us-east-1")));

    private AmazonBedrockConnection() {
    }
}
