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

package com.bytechef.component.wolfram.alpha.shortanswers.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WolframAlphaShortanswersGetShortAnswerAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getShortAnswer")
        .title("Get Short Answer")
        .description("Returns a short answer for your query.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/result"

            ))
        .properties(string("i").label("Query")
            .description("Query that will be answered.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("units").label("Units")
                .description("What system of units to use for measurements and quantities.")
                .options(option("Metric", "metric"), option("Imperial", "imperial"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(string().description("Short answer for query.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.TEXT))));

    private WolframAlphaShortanswersGetShortAnswerAction() {
    }
}
