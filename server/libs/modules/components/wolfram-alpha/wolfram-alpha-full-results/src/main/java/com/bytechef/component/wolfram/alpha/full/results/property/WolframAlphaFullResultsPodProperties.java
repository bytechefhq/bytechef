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

package com.bytechef.component.wolfram.alpha.full.results.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class WolframAlphaFullResultsPodProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("title").label("Title")
            .required(false),
        string("scanner").label("Scanner")
            .required(false),
        string("id").label("Id")
            .required(false),
        integer("position").label("Position")
            .required(false),
        bool("error").label("Error")
            .required(false),
        integer("numsubpods").label("Numsubpods")
            .required(false),
        bool("primary").label("Primary")
            .required(false),
        array("subpods").items(object().properties(WolframAlphaFullResultsSubpodProperties.PROPERTIES))
            .placeholder("Add to Subpods")
            .label("Subpods")
            .required(false),
        object("expressiontypes").properties(WolframAlphaFullResultsExpressionTypesProperties.PROPERTIES)
            .label("Expressiontypes")
            .required(false));

    private WolframAlphaFullResultsPodProperties() {
    }
}
