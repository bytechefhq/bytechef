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

package com.bytechef.component.ai.agent.chat.memory.aws.session.connection;

import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 */
public class AwsSessionChatMemoryConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .controlType(ControlType.PASSWORD)
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .defaultValue("us-east-1")
                        .required(true),
                    string(BUCKET)
                        .label("Bucket")
                        .required(true),
                    string(KEY_PREFIX)
                        .label("Key Prefix")
                        .description("Optional prefix prepended to every session object key.")
                        .required(false)));
}
