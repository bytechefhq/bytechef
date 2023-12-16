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

package com.bytechef.component.jira.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraUserDetailsProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("self").label("Self")
            .description("The URL of the user.")
            .required(false),
        string("name").label("Name")
            .description(
                "This property is no longer available and will be removed from the documentation soon. See the [deprecation notice](https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/) for details.")
            .required(false),
        string("key").label("Key")
            .description(
                "This property is no longer available and will be removed from the documentation soon. See the [deprecation notice](https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-user-privacy-api-migration-guide/) for details.")
            .required(false),
        string("accountId").label("Account Id")
            .description(
                "The account ID of the user, which uniquely identifies the user across all Atlassian products. For example, *5b10ac8d82e05b22cc7d4ef5*.")
            .required(false),
        string("emailAddress").label("Email Address")
            .description(
                "The email address of the user. Depending on the user’s privacy settings, this may be returned as null.")
            .required(false),
        object("avatarUrls").properties(JiraAvatarUrlsBeanProperties.PROPERTIES)
            .placeholder("Add to Avatar Urls")
            .label("Avatar Urls")
            .required(false),
        string("displayName").label("Display Name")
            .description(
                "The display name of the user. Depending on the user’s privacy settings, this may return an alternative value.")
            .required(false),
        bool("active").label("Active")
            .description("Whether the user is active.")
            .required(false),
        string("timeZone").label("Time Zone")
            .description(
                "The time zone specified in the user's profile. Depending on the user’s privacy settings, this may be returned as null.")
            .required(false),
        string("accountType").label("Account Type")
            .description(
                "The type of account represented by this user. This will be one of 'atlassian' (normal users), 'app' (application user) or 'customer' (Jira Service Desk customer user)")
            .required(false));
}
