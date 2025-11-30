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
public class GoogleSearchConsoleApiDimensionFilterProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("dimension")
        .label("Dimension")
        .description(
            "The dimension that this filter applies to. You can filter by any dimension listed here, even if you are not grouping by that dimension.")
        .options(option("QUERY", "QUERY"), option("PAGE", "PAGE"), option("COUNTRY", "COUNTRY"),
            option("DEVICE", "DEVICE"), option("SEARCH_APPEARANCE", "SEARCH_APPEARANCE"))
        .required(true),
        string("operator").label("Operator")
            .description("How your specified value must match (or not match) the dimension value for the row.")
            .options(option("EQUALS", "EQUALS"), option("NOT_EQUALS", "NOT_EQUALS"), option("CONTAINS", "CONTAINS"),
                option("NOT_CONTAINS", "NOT_CONTAINS"), option("INCLUDING_REGEX", "INCLUDING_REGEX"),
                option("EXCLUDING_REGEX", "EXCLUDING_REGEX"))
            .defaultValue("EQUALS")
            .required(true),
        string("expression").label("Expression")
            .description("The value for the filter to match or exclude, depending on the operator.")
            .required(true));

    private GoogleSearchConsoleApiDimensionFilterProperties() {
    }
}
