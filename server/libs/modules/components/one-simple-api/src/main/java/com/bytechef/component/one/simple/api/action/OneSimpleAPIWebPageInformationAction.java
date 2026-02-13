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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.DESCRIPTION;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TITLE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.URL;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class OneSimpleAPIWebPageInformationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("webInformation")
        .title("Web Page Information")
        .description("Get information about a certain webpage")
        .properties(
            string(URL)
                .label("URL")
                .description("Place the web page url you want to get info from")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("general")
                            .properties(
                                string(TITLE),
                                string(DESCRIPTION),
                                string("canonical")),
                        object("twitter")
                            .properties(
                                string("site"),
                                string(TITLE),
                                string(DESCRIPTION)),
                        object("og")
                            .properties(
                                string(TITLE),
                                string("url"),
                                string("image"),
                                string(DESCRIPTION),
                                string("type")))))
        .help("", "https://docs.bytechef.io/reference/components/one-simple-api_v1#web-page-information")
        .perform(OneSimpleAPIWebPageInformationAction::perform);

    private OneSimpleAPIWebPageInformationAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/page_info"))
            .queryParameters(URL, inputParameters.getRequiredString(URL))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
