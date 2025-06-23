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

package com.bytechef.component.apify.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.apify.util.ApifyUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ApifyGetLastRunAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getLastRun")
        .title("Get Last Run")
        .description("Get Apify Actor last run.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/acts/{actorId}/runs/last"

            ))
        .properties(string("actorId").label("Actor ID")
            .description("ID of the actor that will be fetched.")
            .required(true)
            .options((OptionsDataSource.ActionOptionsFunction<String>) ApifyUtils::getActorIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("id").description("The ID of the newly created opportunity.")
            .required(false),
            string("name").description("The name of the newly created opportunity.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ApifyGetLastRunAction() {
    }
}
