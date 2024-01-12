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

import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraHistoryMetadataParticipantProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("The ID of the user or system associated with a history record.")
        .required(false),
        string("displayName").label("Display Name")
            .description("The display name of the user or system associated with a history record.")
            .required(false),
        string("displayNameKey").label("Display Name Key")
            .description("The key of the display name of the user or system associated with a history record.")
            .required(false),
        string("type").label("Type")
            .description("The type of the user or system associated with a history record.")
            .required(false),
        string("avatarUrl").label("Avatar Url")
            .description("The URL to an avatar for the user or system associated with a history record.")
            .required(false),
        string("url").label("Url")
            .description("The URL of the user or system associated with a history record.")
            .required(false));
}
