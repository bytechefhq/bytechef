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
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.PAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.notion.util.NotionUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionCreatePageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPage")
        .title("Create Page")
        .description("Creates a new page that is a child of an existing page.")
        .properties(
            string(ID)
                .label("Parent page ID")
                .options((OptionsFunction<String>) NotionUtils::getPageIdOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The title of the page.")
                .required(false))
        .output(outputSchema(PAGE_OUTPUT_PROPERTY))
        .perform(NotionCreatePageAction::perform);

    private NotionCreatePageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/pages"))
            .body(Http.Body.of(
                Map.of(
                    "parent", Map.of("page_id", inputParameters.getRequiredString(ID)),
                    "properties",
                    Map.of(
                        TITLE, Map.of(
                            TITLE, List.of(Map.of(TEXT, Map.of(CONTENT, inputParameters.getString(TITLE, "")))))))))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
