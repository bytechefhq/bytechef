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

package com.bytechef.component.notion.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.PAGE_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.notion.util.NotionUtils;

/**
 * @author Monika Kušter
 */
public class NotionGetPageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPage")
        .title("Get Page")
        .description("Retrieve page properties using page ID. Response does not contain page content.")
        .properties(
            string(ID)
                .label("Page ID")
                .description("The ID of the page to retrieve.")
                .options((OptionsFunction<String>) NotionUtils::getPageIdOptions)
                .required(true))
        .output(outputSchema(PAGE_OUTPUT_PROPERTY))
        .perform(NotionGetPageAction::perform);

    private NotionGetPageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/pages/%s".formatted(inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
