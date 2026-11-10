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

package com.bytechef.component.sentry.connection;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.sentry.constant.SentryConstants.BASE_URL;
import static com.bytechef.component.sentry.constant.SentryConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.sentry.util.SentryUtils;

/**
 * @author Ivica Cardic
 */
public class SentryConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> SentryUtils.getBaseUri(connectionParameters))
        .help("", "https://docs.bytechef.io/reference/components/sentry_v1#connection-setup")
        .version(1)
        .authorizations(
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Auth Token")
                .properties(
                    string(TOKEN)
                        .label("Auth Token")
                        .description(
                            "A Sentry auth token. Create one in Sentry under Settings -> Developer Settings -> " +
                                "Personal Tokens or Organization Tokens, with the event:read, event:write and " +
                                "project:read scopes.")
                        .required(true),
                    string(BASE_URL)
                        .label("Base URL")
                        .description(
                            "The URL of your Sentry instance: https://sentry.io, a regional URL such as " +
                                "https://us.sentry.io or https://de.sentry.io, or the URL of a self-hosted Sentry.")
                        .controlType(ControlType.URL)
                        .defaultValue(DEFAULT_BASE_URL)
                        .required(true),
                    string(ORGANIZATION)
                        .label("Organization Slug")
                        .description(
                            "The slug of your Sentry organization, as shown in its URL, for example my-org in " +
                                "https://my-org.sentry.io or https://sentry.io/organizations/my-org/.")
                        .required(true)));

    private SentryConnection() {
    }
}
