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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class WolframAlphaFullResultsQueryResultProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        bool("success").label("Success")
            .required(false),
        bool("error").label("Error")
            .required(false),
        integer("numpods").label("Numpods")
            .required(false),
        string("datatypes").label("Datatypes")
            .required(false),
        number("parsetiming").label("Parsetiming")
            .required(false),
        bool("parsetimedout").label("Parsetimedout")
            .required(false),
        string("id").label("Id")
            .required(false),
        string("kernelId").label("Kernel Id")
            .required(false),
        integer("processId").label("Process Id")
            .required(false),
        string("version").label("Version")
            .required(false),
        string("inputstring").label("Inputstring")
            .required(false),
        bool("sbsallowed").label("Sbsallowed")
            .required(false),
        string("parentId").label("Parent Id")
            .required(false),
        string("requestId").label("Request Id")
            .required(false),
        number("timing").label("Timing")
            .required(false),
        string("timedout").label("Timedout")
            .required(false),
        string("timedoutpods").label("Timedoutpods")
            .required(false),
        array("pods").items(object().properties(WolframAlphaFullResultsPodProperties.PROPERTIES))
            .placeholder("Add to Pods")
            .label("Pods")
            .required(false));

    private WolframAlphaFullResultsQueryResultProperties() {
    }
}
