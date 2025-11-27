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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.util.NotionUtils.getDatabase;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.notion.util.NotionUtils;

/**
 * @author Monika Kušter
 */
public class NotionGetDatabaseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDatabase")
        .title("Get Database")
        .description("Retrieve database information by database ID.")
        .properties(
            string(ID)
                .label("Database ID")
                .description("The ID of the database to retrieve.")
                .options(NotionUtils.gePageOrDatabaseIdOptions(false))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("object")
                            .description("The type of the object returned."),
                        string("id")
                            .description("The ID of the database."),
                        string("created_time")
                            .description("The time the page was created."),
                        string("last_edited_time")
                            .description("The time the page was last edited."),
                        string("url")
                            .description("The URL of the database."),
                        array("title")
                            .items(
                                object()
                                    .properties(
                                        string("type"),
                                        object("text")
                                            .properties(
                                                string("content")),
                                        object("annotations")
                                            .properties(
                                                bool("bold")
                                                    .description("Whether the text is bold."),
                                                bool("italic")
                                                    .description("Whether the text is italic."),
                                                bool("strikethrough")
                                                    .description("Whether the text is strikethrough."),
                                                bool("underline")
                                                    .description("Whether the text is underline."),
                                                bool("code")
                                                    .description("Whether the text is code."),
                                                string("color")
                                                    .description("The color of the text.")),
                                        string("plain_text"))),
                        object("parent")
                            .description("The parent of the database.")
                            .properties(
                                string("type")
                                    .description("The type of the parent."),
                                string("page_id")
                                    .description("The ID of the parent page.")),
                        bool("archived")
                            .description("Whether the database is archived."),
                        bool("is_inline"),
                        string("public_url")
                            .description("The public URL of the database."))))
        .perform(NotionGetDatabaseAction::perform);

    private NotionGetDatabaseAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return getDatabase(inputParameters.getRequiredString(ID), context);
    }
}
