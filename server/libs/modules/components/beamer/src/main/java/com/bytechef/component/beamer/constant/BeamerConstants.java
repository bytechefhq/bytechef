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

package com.bytechef.component.beamer.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

public class BeamerConstants {

    public static final String CATEGORY = "category";
    public static final String CONTENT = "content";
    public static final String FEATURE_REQUEST_ID = "requestId";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String POST_ID = "postId";
    public static final String TEXT = "text";
    public static final String USER_ID = "userId";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_FIRST_NAME = "userFirstname";
    public static final String USER_LAST_NAME = "userLastname";

    public static final ModifiableObjectProperty POST_OUTPUT = object()
        .properties(
            array("root")
                .items(string("autoOpen"),
                    string(CATEGORY),
                    string("date"),
                    string("feedbackEnabled"),
                    string(ID),
                    string("published"),
                    string("reactionsEnabled"),
                    array("translations")
                        .items(string(CATEGORY),
                            string(CONTENT),
                            string("contentHtml"),
                            string("language"),
                            string("postUrl"),
                            string(TITLE))));

    public static final ModifiableObjectProperty FEATURE_REQUEST_OUTPUT = object()
        .properties(
            string(ID),
            string("date"),
            string("visible"),
            string(CATEGORY),
            string("status"),
            array("translations")
                .items(
                    string(TITLE),
                    string(CONTENT),
                    string("contentHtml"),
                    string("language"),
                    string("permalink"),
                    string("images")),
            string("votesCount"),
            string("commentsCount"),
            string("notes"),
            string("filters"),
            string("internalUserEmail"),
            string("internalUserFirstname"),
            string("internalUserLastname"),
            string(USER_ID),
            string(USER_EMAIL),
            string(USER_FIRST_NAME),
            string(USER_LAST_NAME),
            string("userCustomAttributes"));

    public static final ModifiableObjectProperty FEATURE_REQUEST_VOTE_OUTPUT = object()
        .properties(
            string(ID),
            string("date"),
            string("featureRequestTitle"),
            string(USER_ID),
            string(USER_EMAIL),
            string(USER_FIRST_NAME),
            string(USER_LAST_NAME),
            string("userCustomAttributes"),
            string("url"));

    public static final ModifiableObjectProperty COMMENT_OUTPUT = object()
        .properties(
            string(ID),
            string("date"),
            string(TEXT),
            string("postTitle"),
            string(USER_ID),
            string(USER_EMAIL),
            string(USER_FIRST_NAME),
            string(USER_LAST_NAME),
            string("url"),
            string("userCustomAttributes"));

    private BeamerConstants() {
    }
}
