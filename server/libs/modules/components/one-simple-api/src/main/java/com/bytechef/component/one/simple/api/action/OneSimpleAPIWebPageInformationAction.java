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

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.ACCESS_TOKEN;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.BASE_URL;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.DESC;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TITLE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.URL;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.WEB_INFORMATION;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Luka Ljubić
 */
public class OneSimpleAPIWebPageInformationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WEB_INFORMATION)
        .title("Web Page Information")
        .description("Get information about a certain webpage")
        .properties(
            string(URL)
                .label("URL")
                .description("Place the web page url you want to get info from")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("general")
                        .properties(
                            string(TITLE),
                            string(DESC),
                            string("canonical")),
                    object("twitter")
                        .properties(
                            string("site"),
                            string(TITLE),
                            string(DESC)),
                    object("og")
                        .properties(
                            string(TITLE),
                            string("url"),
                            string("image"),
                            string(DESC),
                            string("type"))))
        .perform(OneSimpleAPIWebPageInformationAction::perform);

    private OneSimpleAPIWebPageInformationAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context.http(http -> http.get(BASE_URL + "/page_info"))
            .body(
                Context.Http.Body.of(
                    ACCESS_TOKEN, connectionParameters.getRequiredString(ACCESS_TOKEN),
                    URL, inputParameters.getRequiredString(URL),
                    "output", "json"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
