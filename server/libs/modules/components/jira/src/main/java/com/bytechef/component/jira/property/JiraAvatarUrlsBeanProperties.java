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

import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraAvatarUrlsBeanProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("16x16").label("16 X 16")
            .description("The URL of the item's 16x16 pixel avatar.")
            .required(false),
        string("24x24").label("24 X 24")
            .description("The URL of the item's 24x24 pixel avatar.")
            .required(false),
        string("32x32").label("32 X 32")
            .description("The URL of the item's 32x32 pixel avatar.")
            .required(false),
        string("48x48").label("48 X 48")
            .description("The URL of the item's 48x48 pixel avatar.")
            .required(false));
}
