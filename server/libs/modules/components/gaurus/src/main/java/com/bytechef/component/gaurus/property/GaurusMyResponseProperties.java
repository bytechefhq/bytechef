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

package com.bytechef.component.gaurus.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class GaurusMyResponseProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("code").label("Code")
            .description("Represents resulting code in case when the system has handled a request.")
            .options(option("OK", "OK"), option("VALIDATION_FAILED", "VALIDATION_FAILED"),
                option("DATA_UNAVAILABLE", "DATA_UNAVAILABLE"), option("CONSENT_EXPIRED", "CONSENT_EXPIRED"),
                option("GENERAL_ERROR", "GENERAL_ERROR"))
            .required(false),
        string("message").label("Message")
            .description("The error message, present only if the \"code\" property is not \"OK\".")
            .required(false)
            .exampleValue("General error"),
        bool("hasMoreResults").label("Has More Results")
            .description(
                "System limits number of transactions in response. If there are more results related to the request, this flag is set to true. In that case client should initiate a new request with the value of lastTransactionId parameter set to the greatest received transaction identifier plus 1.")
            .required(false)
            .exampleValue(true),
        array("data").items(object().description("List of objects related to the request."))
            .placeholder("Add to Data")
            .label("Data")
            .description("List of objects related to the request.")
            .required(false));

    private GaurusMyResponseProperties() {
    }
}
