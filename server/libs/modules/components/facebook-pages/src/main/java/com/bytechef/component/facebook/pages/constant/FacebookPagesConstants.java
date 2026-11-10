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

package com.bytechef.component.facebook.pages.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.facebook.pages.util.FacebookPagesUtils;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesConstants {

    public static final String GRAPH_API_VERSION = "v26.0";

    public static final String BASE_URL = "https://graph.facebook.com/" + GRAPH_API_VERSION;

    public static final String ACCESS_TOKEN = "access_token";
    public static final String CAPTION = "caption";
    public static final String CREATED_TIME = "created_time";
    public static final String DATA = "data";
    public static final String DESCRIPTION = "description";
    public static final String FILE_URL = "file_url";
    public static final String FROM = "from";
    public static final String ID = "id";
    public static final String LINK = "link";
    public static final String MESSAGE = "message";
    public static final String NAME = "name";
    public static final String PAGE_ID = "pageId";
    public static final String POST_ID = "postId";
    public static final String TITLE = "title";
    public static final String URL = "url";

    public static final String COMMENT_FIELDS = "id,message,from,created_time";

    public static final ModifiableStringProperty PAGE_ID_PROPERTY = string(PAGE_ID)
        .label("Page ID")
        .description("The Facebook Page to use.")
        .options((OptionsFunction<String>) FacebookPagesUtils::getPageIdOptions)
        .required(true);

    public static final ModifiableObjectProperty COMMENT_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the comment."),
            string(MESSAGE)
                .description("The text of the comment."),
            string(CREATED_TIME)
                .description("The time the comment was created."),
            object(FROM)
                .description("The person or Page that made the comment.")
                .properties(
                    string(ID)
                        .description("The ID of the commenter."),
                    string(NAME)
                        .description("The name of the commenter.")));

    private FacebookPagesConstants() {
    }
}
