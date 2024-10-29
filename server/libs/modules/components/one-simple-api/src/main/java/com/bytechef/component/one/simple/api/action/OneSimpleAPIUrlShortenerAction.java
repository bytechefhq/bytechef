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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TOKEN;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.URL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class OneSimpleAPIUrlShortenerAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("urlShortener")
        .title("URL Shortener")
        .description("Shorten your desired URL")
        .properties(
            string(URL)
                .label("URL")
                .description("Place the URL you want to shorten")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(URL),
                        string("short_url"))))
        .perform(OneSimpleAPIUrlShortenerAction::perform);

    private OneSimpleAPIUrlShortenerAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.get("/shortener/new"))
            .body(
                Body.of(
                    TOKEN, connectionParameters.getRequiredString(TOKEN),
                    URL, inputParameters.getRequiredString(URL),
                    "output", "json"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
