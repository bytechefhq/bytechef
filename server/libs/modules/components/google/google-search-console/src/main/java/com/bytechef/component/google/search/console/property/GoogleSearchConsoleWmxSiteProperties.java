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

package com.bytechef.component.google.search.console.property;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class GoogleSearchConsoleWmxSiteProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("permissionLevel").label("Permission Level")
            .description("The user's permission level for the site.")
            .options(option("SITE_PERMISSION_LEVEL_UNSPECIFIED", "SITE_PERMISSION_LEVEL_UNSPECIFIED"),
                option("SITE_OWNER", "SITE_OWNER"), option("SITE_FULL_USER", "SITE_FULL_USER"),
                option("SITE_RESTRICTED_USER", "SITE_RESTRICTED_USER"),
                option("SITE_UNVERIFIED_USER", "SITE_UNVERIFIED_USER"))
            .required(false),
        string("siteUrl").label("Site Url")
            .description("The URL of the site.")
            .required(false));

    private GoogleSearchConsoleWmxSiteProperties() {
    }
}
