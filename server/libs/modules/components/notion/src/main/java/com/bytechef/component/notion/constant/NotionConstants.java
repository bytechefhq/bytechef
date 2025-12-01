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

package com.bytechef.component.notion.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.notion.util.NotionPropertyType;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class NotionConstants {

    public static final String CONTENT = "content";
    public static final String DATABASE_ITEM_ID = "databaseItemId";
    public static final String ID = "id";
    public static final String FIELDS = "fields";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String TEXT = "text";
    public static final String TYPE = "type";

    public static final ModifiableObjectProperty PAGE_OUTPUT_PROPERTY = object()
        .properties(
            string("object")
                .description("The type of the object returned."),
            string("id")
                .description("The ID of the page."),
            string("created_time")
                .description("The time the page was created."),
            string("last_edited_time")
                .description("The time the page was last edited."),
            object("created_by")
                .description("The user who created the page.")
                .properties(
                    string("object")
                        .description("The type of the object returned."),
                    string("id")
                        .description("The ID of the user.")),
            object("last_edited_by")
                .description("The user who last edited the page.")
                .properties(
                    string("object")
                        .description("The type of the object returned."),
                    string("id")
                        .description("The ID of the user.")),
            object("parent")
                .description("The parent of the page.")
                .properties(
                    string("type")
                        .description("The type of the parent."),
                    string("page_id")
                        .description("The ID of the parent page.")),
            bool("archived")
                .description("Whether the page is archived."),
            bool("in_trash")
                .description("Whether the page is in the trash."),
            object("properties")
                .description("The properties of the page.")
                .properties(
                    object(TITLE)
                        .description("The title of the page.")
                        .properties(
                            string("id"),
                            string("type"),
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
                                            string("plain_text"))))),
            string("url")
                .description("The URL of the page."),
            string("request_id")
                .description("The ID of the request."));

    public static final List<NotionPropertyType> SUPPORTED_PROPERTY_TYPES = List.of(
        NotionPropertyType.CHECKBOX,
        NotionPropertyType.DATE,
        NotionPropertyType.EMAIL,
        NotionPropertyType.SELECT,
        NotionPropertyType.MULTI_SELECT,
        NotionPropertyType.STATUS,
        NotionPropertyType.NUMBER,
        NotionPropertyType.PHONE_NUMBER,
        NotionPropertyType.RICH_TEXT,
        NotionPropertyType.TITLE,
        NotionPropertyType.URL);

    private NotionConstants() {
    }
}
