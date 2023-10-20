
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

package com.bytechef.component.jira.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class JiraHistoryMetadataParticipantProperties {
    public static final List<Property> PROPERTIES = List.of(string("id").label("Id")
        .description("The ID of the user or system associated with a history record.")
        .required(false),
        string("displayName").label("DisplayName")
            .description("The display name of the user or system associated with a history record.")
            .required(false),
        string("displayNameKey").label("DisplayNameKey")
            .description("The key of the display name of the user or system associated with a history record.")
            .required(false),
        string("type").label("Type")
            .description("The type of the user or system associated with a history record.")
            .required(false),
        string("avatarUrl").label("AvatarUrl")
            .description("The URL to an avatar for the user or system associated with a history record.")
            .required(false),
        string("url").label("Url")
            .description("The URL of the user or system associated with a history record.")
            .required(false));
}
