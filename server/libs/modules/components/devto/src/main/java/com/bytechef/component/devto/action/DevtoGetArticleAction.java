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

package com.bytechef.component.devto.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.devto.property.DevtoArticleResponseProperties;
import com.bytechef.component.devto.util.DevtoUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DevtoGetArticleAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getArticle")
        .title("Get Article")
        .description("Get article by id.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/articles/{articleId}"

            ))
        .properties(integer("articleId").label("Article ID")
            .description(
                "The unique identifier of the article. Only your articles are listed in the options. See documentation for instructions on how to find ID of any article.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<Long>) DevtoUtils::getArticleIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(DevtoArticleResponseProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private DevtoGetArticleAction() {
    }
}
